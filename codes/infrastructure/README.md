# 本地 Redis 与 MinIO

本目录用于通过 Docker Compose 启动开发环境的 Redis 7 和 MinIO。端口仅绑定到 `127.0.0.1`，不会直接暴露给局域网。

## 1. 创建私有环境文件

在项目根目录的 PowerShell 中执行：

```powershell
Copy-Item -LiteralPath 'infrastructure/.env.example' -Destination 'infrastructure/.env'
```

用 VS Code 或记事本打开 `infrastructure/.env`，替换三个占位符：

- `REDIS_PASSWORD`
- `MINIO_ROOT_USER`
- `MINIO_ROOT_PASSWORD`

不要把实际值发送给 AI。MinIO 密码至少 8 个字符，建议所有本地密码均使用随机强密码。

## 2. 启动服务

确保 Docker Desktop 已启动，然后在项目根目录执行：

```powershell
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.local.yml up -d
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.local.yml ps
```

第一次启动需要下载镜像，耗时取决于网络情况。

## 3. 检查 Redis

```powershell
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.local.yml exec redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping'
```

预期返回：`PONG`。

## 4. 配置 MinIO

1. 浏览器访问 <http://127.0.0.1:9001>。
2. 使用 `infrastructure/.env` 中的 MinIO 用户名和密码登录。
3. 创建名为 `learning-platform` 的存储桶。
4. 保持存储桶为私有，不设置匿名访问策略。

MinIO API 地址为 `http://127.0.0.1:9000`，控制台地址为 `http://127.0.0.1:9001`。

## 5. 停止与重新启动

停止容器但保留数据：

```powershell
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.local.yml stop
```

重新启动：

```powershell
docker compose --env-file infrastructure/.env -f infrastructure/docker-compose.local.yml start
```

不要执行带 `-v` 的 `down` 命令，否则会删除 Redis 和 MinIO 的本地数据卷。

## 6. 操作日志保留任务

MVP 统一将 `operation_log` 保留 180 天。部署后应通过数据库事件、运维平台或受控
计划任务每周执行一次以下清理，并记录执行时间、影响行数和执行人：

```sql
DELETE FROM operation_log
WHERE created_at < CURRENT_TIMESTAMP(3) - INTERVAL 180 DAY;
```

首次启用和每次调整前先完成数据库备份。涉及安全事件、投诉或争议的记录应暂停清理
并受控延期保留，事件关闭后恢复正常周期。完整的数据、内容和隐私规则见
`../backend/docs/mvp-governance-policy.md`。

## 7. 生产部署模板

`production/` 提供后端生产环境变量、systemd 服务和 Nginx 双域名反向代理示例。
这些文件中的域名和凭证均为占位符，不能直接用于公网。完整安装、证书、备份、更新
与回滚步骤见项目根目录 `项目部署.md`。
