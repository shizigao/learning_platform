# 智学云考 HTTP API

## 1. 通用约定

- 开发地址：`http://localhost:8080/api`
- 前端经 Vite/Nginx 同源访问：`/api`
- 除注册、登录、健康检查和公开商品查询外，均使用：

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
X-Request-Id: <客户端请求追踪号>
```

成功和失败都使用统一响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-07-27T08:00:00Z",
  "traceId": "bf3b..."
}
```

分页请求通常使用 `pageNumber`（从1开始）、`pageSize`；分页结果为：

```json
{"items":[],"pageNumber":1,"pageSize":20,"totalElements":0,"totalPages":0}
```

## 2. 错误码

| 业务码 | HTTP | 含义 | 调用方处理 |
| ---: | ---: | --- | --- |
| `0` | 200 | 成功 | 读取 `data` |
| `40000` | 400 | 请求格式或业务参数错误 | 修正参数 |
| `40001` | 400 | Bean Validation 校验失败 | 展示具体字段提示 |
| `40100` | 401 | 未登录或 Token 失效 | 清理会话并跳转登录 |
| `40300` | 403 | 角色、资源权限或额度不足 | 不重试，提示原因 |
| `40400` | 404 | 资源不存在或对当前用户不可见 | 返回列表或错误页 |
| `40900` | 409 | 当前状态不允许操作或幂等任务处理中 | 刷新状态后决定是否重试 |
| `41300` | 413 | 上传超过大小限制 | 重新选择文件 |
| `42900` | 429 | 请求过于频繁 | 按 `Retry-After` 等待 |
| `50000` | 500 | 未预期错误或外部服务失败 | 使用 `traceId` 查日志 |

## 3. 接口索引

“请求/返回”列写 DTO 名称或关键字段；具体字段以同名 Java record 和前端 `src/types` 为准。`USER+` 表示三类登录用户，`PUB+` 表示发布者或管理员。

### 3.1 健康与认证

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/health` | 公开 | 无 | `{status}` |
| POST | `/auth/register` | 公开 | `username,password,nickname,email,phone` | `UserProfile` |
| POST | `/auth/login` | 公开 | `username,password` | `accessToken,expiresIn,user` |
| POST | `/auth/logout` | USER+ | 无 | 空 |
| GET | `/auth/me` | USER+ | 无 | `UserProfile` |
| PUT | `/auth/me` | USER+ | `nickname,email,phone` | `UserProfile` |

### 3.2 用户与个人中心

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/users/search` | USER+ | `keyword,pageNumber,pageSize` | 用户分页 |
| GET | `/users/{userId}` | USER+ | 路径ID | 公开主页与统计 |
| GET | `/users/{userId}/contents` | USER+ | 分页参数 | 公开资料分页 |
| POST | `/users/me/avatar` | USER+ | `multipart file` | 头像信息 |
| DELETE | `/users/me/avatar` | USER+ | 无 | 空 |
| GET | `/users/{userId}/avatar` | USER+ | 无 | 临时访问 URL |

### 3.3 资料查询、学习与互动

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/categories` | USER+ | 无 | 分类列表 |
| GET | `/categories/search` | USER+ | `keyword,pageNumber,pageSize` | 分类分页 |
| GET | `/categories/{categoryId}` | USER+ | 路径ID | 分类详情 |
| GET | `/contents` | USER+ | `keyword,categoryId,isFree,pageNumber,pageSize` | 可访问资料分页 |
| GET | `/contents/{contentId}` | USER+ | 路径ID | 资料详情 |
| GET | `/contents/{contentId}/files/{fileId}/preview-url` | USER+ | 无 | 签名 URL |
| GET | `/contents/{contentId}/files/{fileId}/download-url` | USER+ | 无 | 签名 URL |
| POST | `/learning/contents/{contentId}/start` | USER+ | 无 | 学习进度 |
| PUT | `/learning/contents/{contentId}/progress` | USER+ | `progressPercent` | 学习进度 |
| GET | `/learning/contents/{contentId}/progress` | USER+ | 无 | 学习进度 |
| GET | `/learning/progress` | USER+ | 无 | 我的学习列表 |
| GET | `/learning/favorites` | USER+ | 无 | 收藏资料 |
| GET | `/contents/{contentId}/reactions` | USER+ | 无 | 点赞/收藏状态与计数 |
| POST/DELETE | `/contents/{contentId}/like` | USER+ | 无 | 更新后的互动状态 |
| POST/DELETE | `/contents/{contentId}/favorite` | USER+ | 无 | 更新后的互动状态 |
| POST | `/contents/{contentId}/comments` | USER+ | `content` | 评论 |
| GET | `/contents/{contentId}/comments` | USER+ | 分页参数 | 评论分页 |

### 3.4 发布者资料管理

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/publisher/contents` | PUB+ | 状态、关键词、分页 | 自有资料分页 |
| GET | `/publisher/contents/reference-candidates` | PUB+ | `title,publisher,pageNumber,pageSize` | 可引用资料 |
| POST | `/publisher/contents` | PUB+ | `ContentWriteRequest` | 资料详情 |
| GET | `/publisher/contents/{contentId}` | PUB+ | 路径ID | 编辑详情 |
| PUT | `/publisher/contents/{contentId}` | PUB+ | `ContentWriteRequest` | 资料详情 |
| DELETE | `/publisher/contents/{contentId}` | PUB+ | 无 | 空 |
| POST | `/publisher/contents/{contentId}/submit` | PUB+ | 无 | 待审核资料 |
| POST | `/publisher/contents/{contentId}/files` | PUB+ | multipart，含用途 | 文件元数据 |
| DELETE | `/publisher/contents/{contentId}/files/{fileId}` | PUB+ | 无 | 空 |
| GET | `/publisher/contents/{contentId}/files/{fileId}/preview-url` | PUB+ | 无 | 签名 URL |
| GET | `/publisher/contents/{contentId}/files/{fileId}/download-url` | PUB+ | 无 | 签名 URL |

### 3.5 班级

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/classes` | USER+ | 无 | 我加入的班级 |
| POST | `/classes/join` | USER+ | `inviteCode` | 班级详情 |
| GET | `/classes/{classId}` | USER+ | 无 | 班级详情 |
| POST | `/classes/{classId}/leave` | USER+ | 无 | 空 |
| GET | `/classes/{classId}/members` | USER+ | 无 | 成员列表 |
| GET | `/classes/{classId}/announcements` | USER+ | 无 | 公告列表 |
| GET | `/classes/{classId}/contents` | USER+ | 无 | 班级资料 |
| GET | `/classes/{classId}/exams` | USER+ | 无 | 班级考试 |
| POST | `/classes/{classId}/announcements` | 班级管理者 | `title,content` | 公告 |
| PUT/DELETE | `/classes/{classId}/announcements/{announcementId}` | 班级管理者 | 更新体/无 | 公告/空 |
| GET | `/class-management/classes` | PUB+ | 无 | 可管理班级 |
| POST | `/class-management/classes` | PUB+ | `name,description` | 班级与邀请码 |
| PUT | `/class-management/classes/{classId}` | 班级拥有者 | 基本信息 | 班级详情 |
| POST | `/class-management/classes/{classId}/invite/regenerate` | 拥有者 | 无 | 新邀请码 |
| PUT | `/class-management/classes/{classId}/invite` | 拥有者 | `enabled` | 班级详情 |
| PUT | `/class-management/classes/{classId}/members/{userId}/role` | 拥有者 | `role` | 成员 |
| DELETE/POST | `/class-management/classes/{classId}/members/{userId}`、`.../restore` | 管理者 | 无 | 空/成员 |
| PUT | `/class-management/classes/{classId}/owner` | 拥有者 | `targetUserId` | 班级详情 |
| DELETE | `/class-management/classes/{classId}` | 拥有者 | 无 | 空 |

### 3.6 题库和试卷

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET/POST | `/publisher/question-banks` | PUB+ | 无/`name,description,status` | 列表/题库 |
| PUT/DELETE | `/publisher/question-banks/{bankId}` | PUB+ | 更新体/无 | 题库/空 |
| GET/POST | `/publisher/questions` | PUB+ | 查询参数/`QuestionWriteRequest` | 分页/题目 |
| GET/PUT/DELETE | `/publisher/questions/{questionId}` | PUB+ | 无/更新体/无 | 题目/题目/空 |
| GET/POST | `/publisher/papers` | PUB+ | 查询参数/`name,description` | 分页/试卷 |
| GET/PUT/DELETE | `/publisher/papers/{paperId}` | PUB+ | 无/基本信息/无 | 试卷/试卷/空 |
| PUT | `/publisher/papers/{paperId}/questions` | PUB+ | `{questions:[{questionId,sortOrder,score}]}` | 试卷详情 |

题目答案统一为 `{"acceptedAnswers":[["A"],["别名"]]}`；简答题答案保存在 `text`，选择/判断/填空保存在 `values`。

### 3.7 考试、作答、阅卷

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET/POST | `/publisher/exams` | PUB+ | 查询/`ExamWriteRequest` | 分页/考试 |
| GET/PUT/DELETE | `/publisher/exams/{examId}` | PUB+ | 无/更新体/无 | 管理详情/详情/空 |
| POST | `/publisher/exams/{examId}/publish` | 发布者 | 无 | 已发布考试 |
| POST | `/publisher/exams/{examId}/cancel` | 发布者 | 无 | 已取消考试 |
| GET | `/publisher/exams/quota` | PUB+ | 无 | 剩余次数 |
| GET | `/publisher/exam-candidates` | PUB+ | `keyword` | 候选用户 |
| GET | `/publisher/exam-candidates/search` | PUB+ | 关键词、分页 | 用户分页 |
| GET | `/exams` | USER+ | 无 | 分配给我的考试 |
| GET | `/exams/{examId}`、`/overview`、`/eligibility` | 考生 | 无 | 考试/说明/资格 |
| POST | `/exams/{examId}/start` | 考生 | 无 | 考试会话和题目 |
| GET | `/exams/{examId}/session` | 考生 | 无 | 恢复会话 |
| PUT | `/exams/{examId}/answers/{questionId}` | 考生 | `{values,text}` | 保存答案 |
| PUT | `/exams/{examId}/answers` | 考生 | 批量答案 | 答案列表 |
| POST | `/exams/{examId}/submit` | 考生 | 无 | 提交结果 |
| GET | `/exams/{examId}/result` | 考生 | 无 | 成绩和逐题结果 |
| GET | `/publisher/exams/{examId}/grading/attempts` | 发布者 | 无 | 待阅卷作答 |
| GET | `/publisher/exams/{examId}/grading/attempts/{attemptId}` | 发布者 | 无 | 阅卷详情 |
| PUT | `/publisher/exams/{examId}/grading/attempts/{attemptId}/answers/{answerId}` | 发布者 | `score,comment` | 题目结果 |
| POST | `/publisher/exams/{examId}/grading/attempts/{attemptId}/complete` | 发布者 | 无 | 最终成绩 |
| GET | `/publisher/exams/{examId}/grading/statistics` | 发布者 | 无 | 考试统计 |
| GET | `/exams/wrong-review` | USER+ | 无 | 最近5场错题、额度和报告 |
| POST | `/exams/wrong-review/analysis` | USER+ | `requestId` | 错题 AI 报告 |

### 3.8 AI

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| POST | `/ai/contents/{contentId}/summaries` | 有资料权 | `requestId` | 总结 |
| GET | `/ai/contents/{contentId}/summaries/latest` | 有资料权 | 无 | 最近总结 |
| GET | `/ai/tasks`、`/tasks/{taskId}` | USER+ | 无 | 本人任务 |
| GET | `/ai/usage-records` | USER+ | 无 | 扣次记录 |
| POST/GET | `/ai/contents/{contentId}/conversations` | 有资料权 | `title`/无 | 会话/列表 |
| GET | `/ai/conversations/{conversationId}` | 会话所有者 | 无 | 消息详情 |
| POST | `/ai/conversations/{conversationId}/messages` | 会话所有者 | `requestId,question` | 用户消息和 AI 回答 |
| POST | `/ai/conversations/{conversationId}/templates` | 会话所有者 | `requestId,template` | 模板消息和 AI 回答 |
| GET/POST | `/publisher/exams/{examId}/grading/ai-analysis` | 考试发布者 | 无/`requestId` | 整体分析页/报告 |
| GET/POST | `/exams/{examId}/result/ai-analysis` | 考生 | 无/`requestId` | 个人分析页/报告 |

模板值：`QUIZ_REINFORCEMENT`、`DIVERGENT_THINKING`。AI 请求只有在结果成功持久化后扣1次；同一 `requestId` 重放返回原结果。

### 3.9 商品、订单和权益

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/products`、`/products/{productId}` | USER+ | 类型过滤/无 | 商品列表/详情 |
| POST | `/orders` | USER+ | 商品ID、数量或资料目标 | 订单 |
| GET | `/orders`、`/orders/{orderId}` | USER+ | 无 | 本人订单 |
| POST | `/orders/{orderId}/cancel` | 订单所有者 | 无 | 订单 |
| POST | `/orders/{orderId}/mock-pay` | 订单所有者 | 无 | 模拟支付结果 |
| GET | `/entitlements` | USER+ | 无 | 权益列表 |
| GET | `/entitlements/balances` | USER+ | 无 | 各类剩余次数 |

### 3.10 线下教学

| 方法 | 地址 | 权限 | 请求 | 返回 |
| --- | --- | --- | --- | --- |
| GET | `/offline-teaching/teachers` | USER+ | 地区、内容、价格、分页 | 教师分页 |
| GET | `/offline-teaching/teachers/{teacherId}` | USER+ | 无 | 公开详情 |
| GET | `/offline-teaching/teachers/by-user/{userId}` | USER+ | 无 | 账户对应教师 |
| GET/PUT | `/offline-teaching/application` | PUB+ | 无/完整申请信息 | 本人申请 |
| POST | `/offline-teaching/application/submit` | PUB+ | 无 | 已提交申请 |
| GET/PUT | `/offline-teaching/preference` | USER+ | 无/学习目标、地区、预算、时间 | 推荐偏好 |
| POST | `/offline-teaching/recommendations` | USER+ | `requestId` | 至多3名 AI 推荐 |

推荐流程先按地区、预算、教授内容和可上课时间在本地选至多20名，再交给 AI 排序。

### 3.11 管理后台

| 方法 | 地址 | 权限 | 作用 |
| --- | --- | --- | --- |
| GET | `/admin/users`、`/admin/users/{userId}` | ADMIN | 用户列表与详情 |
| PUT | `/admin/users/{userId}/status`、`/roles` | ADMIN | 状态与角色 |
| GET/POST/PUT/DELETE | `/admin/categories[/{id}]` | ADMIN | 分类管理 |
| GET | `/admin/contents`、`/admin/contents/{contentId}` | ADMIN | 资料审核查询 |
| POST | `/admin/contents/{id}/approve|reject|offline|publish` | ADMIN | 资料状态流转 |
| GET | `/admin/exams`、`/admin/exams/{examId}` | ADMIN | 考试监管 |
| GET | `/admin/orders`、`/admin/orders/{orderId}` | ADMIN | 订单监管 |
| GET | `/admin/operation-logs` | ADMIN | 操作审计 |
| GET | `/admin/ai/config` | ADMIN | 脱敏 AI 运行配置 |
| GET | `/admin/offline-teachers/applications[/{id}]` | ADMIN | 教师申请 |
| GET | `/admin/offline-teachers/profiles/by-user/{userId}` | ADMIN | 教师档案 |
| POST | `/admin/offline-teachers/applications/{id}/approve|reject` | ADMIN | 审核 |
| PUT | `/admin/offline-teachers/profiles/{id}/suspend|activate` | ADMIN | 教师状态 |

## 4. 调用示例

### 4.1 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"Password123"}'
```

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJ...",
    "expiresIn": 7200,
    "user": {"id": 2, "username": "demo", "roles": ["USER"]}
  }
}
```

### 4.2 保存学习进度

```bash
curl -X PUT http://localhost:8080/api/learning/contents/12/progress \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"progressPercent":51}'
```

### 4.3 保存考试答案

```bash
curl -X PUT http://localhost:8080/api/exams/8/answers/31 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"values":["A","B"],"text":null}'
```

### 4.4 AI 会话模板

```bash
curl -X POST http://localhost:8080/api/ai/conversations/5/templates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"requestId":"quiz-20260727-001","template":"QUIZ_REINFORCEMENT"}'
```

### 4.5 错题 AI 分析

```bash
curl -X POST http://localhost:8080/api/exams/wrong-review/analysis \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"requestId":"wrong-review-20260727-001"}'
```

AI 超时或供应商失败时返回安全错误消息和 `traceId`，不会扣除额度。
