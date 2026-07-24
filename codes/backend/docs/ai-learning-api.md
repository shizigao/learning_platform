# AI 资料总结与知识讲解接口

## 实现范围

本文档对应阶段 H 的 OP-148～OP-155。当前 AI 能力只处理文本，不解析图片、
音频或视频，也不对 PDF、Office 等二进制文档做文字识别。

用于 AI 的资料文本按以下顺序组合：

1. 资料标题；
2. 资料简介；
3. 图文正文；
4. 文件角色为 `CONTENT` 或 `ATTACHMENT`、UTF-8 编码且不超过 2 MiB 的
   `.txt`、`.md` 文件。

封面、图片、视频、字幕文件和其他格式均会被忽略。超过配置输入上限的资料会被拒绝，
不会静默截断。

所有接口均要求登录，并沿用资料访问权限：免费资料可直接使用，付费资料需已购买，
发布者和管理员可访问。用户只能查询自己的 AI 任务和会话。

## 资料总结

生成总结：

```http
POST /api/ai/contents/{contentId}/summaries
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "requestId": "summary-20260724-001"
}
```

`requestId` 可省略；提供时最长 64 个字符，用于防止网络重试造成重复任务。同一用户、
同一资料和同一任务类型重复提交相同 `requestId`，会返回首次生成的结果。

响应包含：

- `task`：任务编号、供应商、模型、状态和时间；
- `summary`：资料摘要；
- `knowledgePoints`：核心知识点列表；
- `reviewOutline`：复习提纲；
- `sourceVersion`：本次输入文本的 SHA-256 版本。

查询当前用户对某资料最近一次成功总结：

```http
GET /api/ai/contents/{contentId}/summaries/latest
Authorization: Bearer <access-token>
```

查询单个任务或本人的任务记录：

```http
GET /api/ai/tasks/{taskId}
GET /api/ai/tasks
Authorization: Bearer <access-token>
```

任务状态依次为 `PENDING`、`RUNNING`、`SUCCEEDED` 或 `FAILED`。生成结果与任务成功
状态在同一事务中保存；调用或解析失败时任务会记录为 `FAILED`，并向前端返回安全的
通用错误信息。

## 知识讲解

创建资料讲解会话：

```http
POST /api/ai/contents/{contentId}/conversations
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "title": "数据库事务复习"
}
```

`title` 可省略，系统会根据资料标题生成默认名称。

提交问题：

```http
POST /api/ai/conversations/{conversationId}/messages
Authorization: Bearer <access-token>
Content-Type: application/json

{
  "requestId": "explain-20260724-001",
  "question": "请用简单语言解释 ACID"
}
```

系统以当前资料文本为依据，并按配置的消息数和字符数上限携带最近会话上下文。问题和 AI 回答
分别以 `USER`、`ASSISTANT` 消息持久化；同一讲解请求重复提交相同 `requestId`
不会重复写入消息。

查询资料下的本人会话或单个会话详情：

```http
GET /api/ai/contents/{contentId}/conversations
GET /api/ai/conversations/{conversationId}
Authorization: Bearer <access-token>
```

会话详情按 `sequenceNo` 返回消息历史。访问其他用户的任务或会话统一按不存在处理，
避免泄露其编号和内容。

## 次数扣减与使用记录

每个新总结或讲解任务默认消耗 1 次 `AI_QUOTA`。系统在调用前检查可用次数，但不会
预扣；只有 AI 调用成功且结果能够保存时，才会在同一数据库事务中完成以下操作：

1. 保存总结或会话消息；
2. 锁定并扣减 AI 次数权益；
3. 写入唯一的 `ai_usage_record`；
4. 将任务更新为 `SUCCEEDED`。

任一步骤失败都会整体回滚，不保存不完整结果，也不扣次数。同一个 `requestId`
重复提交时返回原任务和结果，不产生第二条使用记录。额度不足会建立安全的失败任务，
但不会调用 AI 或写入使用记录。

查询本人使用记录：

```http
GET /api/ai/usage-records
Authorization: Bearer <access-token>
```

每条记录包含任务、权益、使用类型、扣减数量及扣减前后余额。当前余额也可继续通过
`GET /api/entitlements/balances` 查询。

## 调用限制

以下限制可通过环境变量调整，配置错误会阻止应用启动：

| 环境变量 | 默认值 | 作用 |
| --- | ---: | --- |
| `AI_MAX_INPUT_CHARS` | `100000` | 单份资料组合文本最大字符数 |
| `AI_MAX_CONTEXT_MESSAGES` | `10` | 讲解请求最多携带的历史消息数 |
| `AI_MAX_CONTEXT_CHARS` | `20000` | 讲解历史消息最大总字符数 |
| `AI_REQUESTS_PER_WINDOW` | `10` | 单用户在限流窗口内最多发起的新任务数 |
| `AI_RATE_WINDOW_SECONDS` | `60` | 调用频率窗口秒数 |
| `AI_MAX_CONCURRENT_PER_USER` | `1` | 单用户同时执行的 AI 调用数 |
| `AI_REQUEST_TIMEOUT_SECONDS` | `630` | 统一 AI 调用总超时秒数 |
| `DEEPSEEK_CONNECT_TIMEOUT_SECONDS` | `10` | 供应商 TCP/TLS 连接超时秒数 |
| `DEEPSEEK_TIMEOUT_SECONDS` | `620` | 供应商响应等待超时秒数 |

频率和并发超限返回 HTTP 429；统一调用超时会记录失败任务。供应商的连接超时与
响应等待分别配置；响应等待按 DeepSeek 最长 10 分钟排队保活机制设置，并短于统一
任务超时。
真实学习任务默认设置 `DEEPSEEK_THINKING_ENABLED=false`，减少总结和连续讲解的
生成延迟。

## 安全错误

任务只保存归一化的错误代码和可安全展示的简短信息。供应商响应正文、鉴权信息、
异常堆栈、数据库错误和 API Key 不会通过接口返回。前端只能读取本人任务与使用记录。

## 前端页面

- `/ai-assistant`：登录用户的 AI 学习助手。支持按资料生成总结、查看知识点和复习
  提纲、创建讲解会话、继续提问、查看会话历史、当前额度与最近使用记录。
- `/admin/ai`：仅管理员可访问的 AI 配置页。页面只读取生效的供应商、模型、Mock
  场景和调用限制，不允许在浏览器录入或读取 API Key。
- 资料详情页和“我的学习”均提供 AI 学习助手入口；从资料详情进入时会自动选中资料。
- 生成和讲解期间页面会依次显示提交、生成、长时间等待、成功或明确失败状态；
  三列内容使用固定高度和独立滚动条。前端请求超时为 645 秒，后端仍以
  `AI_REQUEST_TIMEOUT_SECONDS` 为最终任务超时。

管理员可通过以下接口读取脱敏后的生效配置：

```http
GET /api/admin/ai/config
Authorization: Bearer <admin-access-token>
```

响应只包含 `apiKeyConfigured` 布尔值，不返回 API Key；普通用户访问返回 403。

## Mock 场景

默认使用成功场景：

```text
AI_PROVIDER=mock
AI_MOCK_SCENARIO=success
AI_MOCK_DELAY_MILLIS=0
```

`AI_MOCK_SCENARIO` 支持：

| 值 | 行为 | 用途 |
| --- | --- | --- |
| `success` | 返回稳定的模拟总结或讲解 | 正常流程、幂等与扣费测试 |
| `failure` | 抛出模拟供应商失败 | 验证失败任务和不扣次数 |
| `timeout` | 配合延迟触发统一超时 | 验证超时任务和不扣次数 |

测试超时时，应让 `AI_MOCK_DELAY_MILLIS` 大于
`AI_REQUEST_TIMEOUT_SECONDS × 1000`。修改环境变量后需重启后端。

默认 `AI_PROVIDER=mock`，可使用 OP-147 的 DeepSeek 配置切换真实客户端。

## 自动化验证

- `ContentTextExtractorTests`：验证正文和 UTF-8 文本文件参与提取、媒体被忽略及
  超长输入被拒绝。
- `AiRequestGuardTests`：验证频率、同用户并发和统一超时限制。
- `AiConversationPromptFactoryTests`：验证历史消息数及上下文字符上限。
- `AiLearningControllerIntegrationTests`：验证总结持久化、任务状态、幂等请求、
  会话消息、无额度、原子回滚、只扣一次、使用记录、管理员脱敏配置及跨用户数据隔离。
- `MockAiFailureIntegrationTests`：验证供应商失败时任务失败、错误脱敏且额度不变。
- `MockAiTimeoutIntegrationTests`：验证统一超时时任务失败、无结果且额度不变。
- `AiClientTests`：验证 mock 与 DeepSeek 客户端选择和安全配置。
