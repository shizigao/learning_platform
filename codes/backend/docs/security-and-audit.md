# OP-171～OP-172 操作审计与接口安全

## 关键操作审计

关键操作由统一过滤器在请求完成后写入 `operation_log`。即使业务返回 4xx/5xx，
也会留下失败结果，便于按请求 ID 关联应用日志。审计写入使用独立事务，写入失败
不会覆盖原业务响应。

记录范围：

- 登录成功和失败；
- 管理员变更账号状态或角色；
- 资料审核通过、驳回、发布和下架；
- 订单创建、取消、模拟支付与权益发放；
- 考试发布；
- 人工评分与完成阅卷。

日志只保存操作元数据。请求体、响应体、密码、JWT、API Key 和文件正文均不进入
审计表。管理员通过 `GET /api/admin/operation-logs` 或管理后台“操作日志”页查询。

MVP 的操作日志保留周期确定为 **180 天**。部署环境应每周清理
`created_at` 早于 180 天的记录；安全事件或争议涉及的日志可以受控延期保留。
具体清理 SQL、用户内容规范和隐私告知文本见
[`mvp-governance-policy.md`](mvp-governance-policy.md)。

## 接口限流

默认按服务端观察到的客户端 IP 使用 60 秒固定窗口，并对三类请求使用独立额度：

| 类别 | 默认额度 | 配置项 |
| --- | ---: | --- |
| 一般 `/api/**` 请求 | 600 次/分钟 | `API_GENERAL_REQUESTS_PER_MINUTE` |
| 登录和注册 | 30 次/分钟 | `API_AUTH_REQUESTS_PER_MINUTE` |
| 文件上传请求 | 30 次/分钟 | `API_UPLOAD_REQUESTS_PER_MINUTE` |

超过额度返回 HTTP 429、业务码 `42900` 和 `Retry-After`。健康检查和 CORS OPTIONS
不计入额度。可用 `API_RATE_LIMIT_ENABLED=false` 在受控测试环境临时关闭。

当前实现适用于单实例部署；多实例部署时应把计数器迁移到 Redis 或网关，并仅在
可信反向代理完成来源地址校验后使用代理转发的客户端 IP。

## 上传约束

上传元数据在获取 MinIO 上传许可前检查：

- 文件扩展名与 MIME 类型必须同时位于对应用途的白名单且相互匹配；
- 上传流还会校验 JPEG、PNG、WebP、PDF、ZIP/Office、MP4、WebM 和文本类文件头，
  防止将可执行文件仅通过改名和伪造 MIME 上传；
- 文件名不得包含路径分隔符或控制字符，对象路径由服务端 UUID 生成；
- 封面 10 MB、正文 50 MB、字幕 5 MB，视频和附件最多 200 MB；
- 每份资料最多 20 个文件，并有按用途数量上限；
- 非管理员不能向其他用户的资料上传文件。

Spring Multipart 层另设 200 MB 单文件和 220 MB 单请求上限。

## 响应头与敏感字段

Spring Security 对 API 响应显式设置：

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`
- 限制性 `Content-Security-Policy`

登录 DTO、JWT、MinIO 和 AI 配置的字符串表示已脱敏；管理接口不返回密码哈希、
最后登录 IP 或 API Key。Spring MVC 的请求/响应对象 DEBUG 输出固定为 INFO，
防止通过对象 `toString()` 泄露敏感正文。
