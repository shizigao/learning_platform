# Redis 使用说明

## 1. 定位与可靠性边界

Redis 是共享加速层和短期运行状态存储，不是订单、权益、考试答案、成绩或班级权限的
唯一数据源。Redis 不可用时，系统会回退到本机限流或 MySQL 查询；正式考试答案、
最终成绩和权益扣减始终同步写入 MySQL。

## 2. 当前用途

| 用途 | 主要 Key | TTL/清理 | Redis 故障策略 |
| --- | --- | --- | --- |
| 登录失败防刷 | `auth:login:*` | 15 分钟窗口 | 记录告警并允许回源登录校验 |
| API 固定窗口限流 | `rate:api:*` | 当前分钟剩余时间 | 回退单实例内存计数 |
| AI 滑动窗口 | `rate:ai:user:*` | 限流窗口 | 回退本机滑动窗口 |
| AI 并发租约 | `lease:ai:user:*` | AI 超时两倍加 30 秒 | 回退本机信号量 |
| JWT 认证快照 | `lp:v1:auth:snapshot:*` | 60 秒 | 回查用户与角色表 |
| 资料/考试授权判断 | `lp:v1:authz:*` | 判断 30 秒；版本长期 | 回查 MySQL，禁止未知权限放行 |
| 分类、头像、公开主页 | `lp:v1:content:*`、`lp:v1:user:*` | 2～30 分钟 | 回查 MySQL/MinIO |
| 试卷题目与考试统计 | `lp:v1:exam:*` | 20 秒～30 分钟 | 重新执行数据库查询 |
| 资料浏览量缓冲 | `content:view-count:pending` | 定时批量落库 | 直接更新 MySQL |
| 考试截止索引 | `exam:attempt:deadlines` | 提交后移除 | 定期 MySQL 扫描兜底 |

缓存写路径在数据库事务提交后主动失效。授权缓存采用“用户版本 + 资源版本”，成员、
权益或资源范围变化时递增版本，避免使用通配扫描删除 Key。

## 3. 关键一致性规则

- 点赞、收藏关系继续依赖 MySQL 唯一约束，Redis 不作为关系真源。
- 浏览量允许短暂最终一致；接口返回值会合并 MySQL 基数和 Redis 待回写增量。
- 超时交卷最终仍使用 MySQL 行锁和状态条件判断，Redis 只负责快速发现到期作答。
- AI 次数、考试发布次数和付费资料权益仍由 MySQL 事务原子扣减。
- 账号禁用和角色变更在事务提交后删除认证快照，60 秒 TTL 是异常兜底。

## 4. 配置

配置模板位于 `backend/.env.example` 和
`infrastructure/production/backend.env.example`。可分别关闭分布式守卫、读取缓存、
授权缓存和浏览量缓冲，以便故障排查；生产环境通常保持启用。

## 5. 验证与排障

```bash
redis-cli -a "$REDIS_PASSWORD" ping
redis-cli -a "$REDIS_PASSWORD" --scan --pattern 'lp:v1:*'
redis-cli -a "$REDIS_PASSWORD" ZCARD exam:attempt:deadlines
redis-cli -a "$REDIS_PASSWORD" HGETALL content:view-count:pending
```

`PING` 应返回 `PONG`。应用 readiness 同时检查 Redis；缓存连接异常时，后端会输出
包含具体用途的降级日志。不要使用 `KEYS *` 检查生产实例。
