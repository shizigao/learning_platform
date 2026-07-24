# AI 客户端与供应商切换

## 适用范围

本文档对应阶段 H 的 OP-147。业务服务只依赖 `AiClient`，不直接引用
DeepSeek 或模拟实现，从而允许后续资料总结和知识讲解复用同一调用入口。

## 统一接口

`AiClient` 提供以下能力：

- `provider()`：当前供应商代码。
- `model()`：当前模型标识。
- `complete(AiClientRequest)`：使用统一消息列表发起文本生成。

统一请求支持 `SYSTEM`、`USER`、`ASSISTANT` 三种消息角色，以及可选的最大输出
Token 数、temperature 和 `TEXT`/`JSON_OBJECT` 响应格式。统一响应包含供应商、
模型、供应商请求 ID、正文、结束原因和 Token 用量。业务层不需要解析供应商
原始 JSON。

## 供应商选择

默认配置：

```text
AI_PROVIDER=mock
AI_MOCK_MODEL=mock-learning-assistant-v1
```

默认的 `MockAiClient` 不访问网络、不需要 API Key，会返回带有“模拟 AI”标识的
确定性格式结果，供 OP-148～OP-155 的开发和自动化测试使用。

需要真实联调时，由用户在 IDEA 运行配置或操作系统环境变量中设置：

```text
AI_PROVIDER=deepseek
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_API_KEY=<仅保存在本机的真实密钥>
DEEPSEEK_MODEL=deepseek-v4-flash
DEEPSEEK_CONNECT_TIMEOUT_SECONDS=10
DEEPSEEK_TIMEOUT_SECONDS=620
DEEPSEEK_THINKING_ENABLED=false
```

切换到 `deepseek` 但未配置 `DEEPSEEK_API_KEY` 时，应用会在启动阶段快速失败，
避免误以为正在调用真实模型。未知的 `AI_PROVIDER` 同样会拒绝启动。

## DeepSeek 实现

`DeepSeekAiClient` 使用官方 OpenAI 兼容接口：

```http
POST {DEEPSEEK_BASE_URL}/chat/completions
Authorization: Bearer <DEEPSEEK_API_KEY>
Content-Type: application/json
```

官方接口文档：
<https://api-docs.deepseek.com/api/create-chat-completion>

客户端使用 JDK HTTP Client（HTTP/1.1）请求一次性 JSON 响应，并安全映射正文、
finish reason 和 usage。业务接口本身需要完整结果后才返回前端，因此不使用无法
传递给前端且曾造成 SSE 读取阻塞的供应商流式模式。学习总结和讲解默认显式关闭
模型思考模式，避免简单任务产生不必要的推理等待；确有需要时可通过
`DEEPSEEK_THINKING_ENABLED=true` 开启。资料总结请求
同时发送 `response_format: {"type":"json_object"}`，并在系统提示中明确要求
JSON，以符合供应商结构化输出协议；知识讲解保持普通文本响应。401/403、429、
超时、供应商错误和无效响应被转换为不包含响应正文、请求正文或 API Key 的安全
异常类型。

连接建立与响应等待使用独立超时：TCP/TLS 连接默认只等待 10 秒；完整响应的等待
上限为 620 秒。统一任务超时为 630 秒，前端为 645 秒，保证各层超时顺序一致。

## 密钥安全

- API Key 只从 `DEEPSEEK_API_KEY` 环境变量读取。
- 前端没有 DeepSeek 配置，也不会接触 API Key。
- `.env.example` 只保留明显占位符，不保存真实值。
- 客户端异常不拼接供应商响应正文，不输出 Authorization 请求头。
- 默认使用 mock，不会意外产生真实调用和费用。

## 诊断日志

后端启动和真实调用会输出不含密钥与正文的结构化诊断日志：

- `AI_CLIENT_CONFIG`：启动时实际生效的供应商、模型、Base URL、是否配置密钥、
  Mock 场景以及两层超时。
- `AI_PROVIDER_START`：准备向 DeepSeek 发送请求，只记录消息数、总字符数和输出
  Token 上限，并标明实际传输实现和 `stream=false`。
- `AI_PROVIDER_HEADERS`：已连接供应商并收到 HTTP 状态。
- `AI_PROVIDER_SUCCESS` / `AI_PROVIDER_FAILURE`：完成信息或安全归一化后的失败原因。
- `AI_SUMMARY_FAILURE` / `AI_EXPLANATION_FAILURE`：失败位于调用限制、供应商、
  业务校验还是结果处理阶段。

Web 请求的 `traceId` 会传播到 AI 工作线程，可使用浏览器响应中的 traceId 在控制台
筛选同一次调用。日志不会打印 API Key、Authorization、用户 Token、资料正文、
问题正文或 AI 完整回答。

## 自动化验证

`AiClientTests` 覆盖：

- 未提供真实密钥时可正常选择 mock。
- 显式配置后选择 DeepSeek 实现。
- DeepSeek 缺少密钥和未知供应商时快速失败。
- DeepSeek Chat Completion 的 URL、鉴权头、结构化输出参数、响应正文及 Token
  用量映射。
