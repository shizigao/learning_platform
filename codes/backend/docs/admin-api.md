# 阶段 I 管理后台接口

所有接口均位于 `/api/admin/**`，必须携带有效管理员 JWT；普通用户和发布者访问时
返回 HTTP 403。

## 用户管理

- `GET /api/admin/users`：按 `keyword`、`status`、`role` 分页查询用户。
- `GET /api/admin/users/{userId}`：查询用户及其角色。
- `PUT /api/admin/users/{userId}/status`：请求体为
  `{"status":"ACTIVE|DISABLED"}`，启用或禁用账号。
- `PUT /api/admin/users/{userId}/roles`：请求体为
  `{"roles":["USER","PUBLISHER"]}`，以给定集合替换用户角色。

用户响应不包含密码哈希和最后登录 IP。账号禁用后，认证过滤器会在每次请求时重新
读取用户状态，因此已经签发的 JWT 也会立即失效。系统拒绝管理员禁用自己的当前
账号，也拒绝移除自己的 `ADMIN` 角色，避免管理权限被误操作锁死。

## 考试查看

- `GET /api/admin/exams`：按 `keyword`、`publisherId`、`status` 分页查看所有
  发布者的考试。
- `GET /api/admin/exams/{examId}`：查看考试、试卷摘要、发布者和指定考生信息。

管理员考试功能为全局只读视图。考试的编辑、发布、取消、阅卷等业务操作继续通过
原有发布者接口执行并沿用资源归属校验。

## 已有管理功能

- 资料审核、驳回、发布、下架：`/api/admin/contents/**`
- 分类维护：`/api/admin/categories/**`
- 模拟订单及支付记录查看：`/api/admin/orders/**`
- 脱敏 AI 配置查看：`/api/admin/ai/config`

AI API Key 仅通过后端环境变量配置，管理页面只显示“是否已配置”，不返回密钥值，
也不支持在浏览器中修改真实密钥。

## 操作日志

- `GET /api/admin/operation-logs`：按 `operatorId`、`module`、`action`、
  `result`、`requestId` 分页查询关键操作。

当前记录登录、账号状态和角色变更、资料审核/发布/下架、订单创建/取消/模拟支付及
权益发放、考试发布、人工评分和完成阅卷。日志包含操作人、目标、请求路径、请求 ID、
来源 IP、User-Agent、HTTP 结果和耗时；不保存密码、JWT、请求体或响应体。

管理后台“操作日志”页提供相同筛选条件。普通用户和发布者不能查询日志。
