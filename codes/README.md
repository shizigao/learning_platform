# 智学云考——智能在线学习考试平台

智学云考是一个面向学习者、内容发布者和管理员的前后端分离学习平台。项目围绕
“学习资料—在线考试—权益购买—AI 学习辅助—后台治理”形成完整 MVP 闭环。

当前支付功能为**模拟支付**，不会产生真实资金交易；AI 支持本地 Mock 和 DeepSeek
真实接口两种模式。

## 1. 主要功能

- 用户注册、登录、个人资料和基于 JWT 的角色权限；
- 学习资料创建、文件上传、审核、发布、下架、检索和付费访问；
- 学习进度、点赞、收藏、评论和“我的学习”；
- 题库、固定试卷、考试发布、指定考生、自动交卷、客观题判分和人工阅卷；
- 商品、模拟订单、模拟支付及资料、AI 次数、考试发布次数三类权益；
- 资料总结、知识点、复习提纲、AI 讲解会话和额度记录；
- 用户、资料、考试、订单、AI 配置及操作日志管理；
- 限流、上传白名单、敏感字段脱敏、安全响应头和权限回归测试。

## 2. 技术架构

| 部分 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 21 运行环境、Spring Boot 3.5、Spring Security、MyBatis、JWT |
| 数据库 | MySQL 8.0 |
| 缓存与防刷 | Redis 7 |
| 文件存储 | MinIO |
| AI | Mock AI / DeepSeek Chat Completion |
| 构建 | Maven 3.9、Node.js 22、npm 10 |

后端按 Java 17 目标版本编译，推荐直接使用已经验证过的 JDK 21 运行。开发时前端
通过 Vite 的 `/api` 代理连接 `http://localhost:8080`；部署时由 Nginx 同源提供
前端文件并反向代理 `/api/`。

## 3. 目录结构

```text
codes/
├─ backend/                         Spring Boot 后端
│  ├─ docs/                        接口、安全与治理说明
│  ├─ src/main/                    生产代码和配置
│  ├─ src/test/                    单元及集成测试
│  └─ .env.example                 后端环境变量模板
├─ frontend/learning_platform_frontend/
│  ├─ src/                         Vue 前端源码
│  ├─ tests/                       前端逻辑测试
│  └─ .env.example                 前端公开变量模板
├─ database/
│  ├─ 001_schema.sql               数据库结构
│  ├─ 002_seed_data.sql            基础角色、商品和配置
│  └─ README.md                    数据库初始化细节
├─ infrastructure/
│  ├─ docker-compose.local.yml     本地 Redis、MinIO
│  └─ production/                  生产部署模板
├─ 环境配置说明.md
├─ 项目部署.md
└─ 操作流程.md
```

## 4. 开发环境准备

以下版本已经在本项目中验证：

| 软件 | 推荐版本 | 验证命令 |
| --- | --- | --- |
| Git | 当前稳定版 | `git --version` |
| JDK | 21 LTS | `java -version` |
| Maven | 3.9.x | `mvn -version` |
| Node.js | 22.x LTS | `node -v` |
| npm | 10.x | `npm -v` |
| MySQL | 8.0.x | `mysql --version` |
| Docker Desktop / Engine | 支持 Compose v2 | `docker compose version` |

### 4.1 安装 Git

Windows 可安装 Git for Windows。安装完成后重新打开 PowerShell：

```powershell
git --version
git config --global core.quotepath false
```

第二条命令用于让 Git 更友好地显示中文路径，不影响仓库内容。

### 4.2 安装并配置 JDK

安装 JDK 21，将安装目录设置为 `JAVA_HOME`，并将 `%JAVA_HOME%\bin` 加入 `Path`。
重新打开终端后检查：

```powershell
java -version
javac -version
$env:JAVA_HOME
```

三项都应指向同一套 JDK。IDEA 中还需在 `Project SDK` 和 Maven Runner 中选择该 JDK。

### 4.3 安装 Maven

解压 Maven 3.9.x，设置 `MAVEN_HOME`，将 `%MAVEN_HOME%\bin` 加入 `Path`：

```powershell
mvn -version
```

输出中的 Java home 应与上一节的 JDK 一致。首次构建会下载依赖，需要能够访问配置的
Maven 仓库。

### 4.4 安装 Node.js 与 npm

安装 Node.js 22.x LTS；npm 会随 Node 一起安装：

```powershell
node -v
npm -v
```

不要提交 `node_modules`。前端依赖应使用仓库中的 `package-lock.json` 还原。

### 4.5 安装 MySQL 8

安装 MySQL Server 8.0，启用 `utf8mb4`，记住本机管理员密码。确认 Windows 服务已
启动并能连接：

```powershell
Get-Service MySQL*
mysql --user=root --password
```

进入 MySQL 后执行：

```sql
SELECT VERSION();
SHOW VARIABLES LIKE 'character_set_server';
```

版本应为 8.0.x。若命令行工具未加入 `Path`，也可使用 MySQL Workbench 或 IDEA
Database 完成后续初始化。

### 4.6 安装 Docker Desktop

Windows 推荐启用 WSL 2 后安装 Docker Desktop，并确认 Linux Containers 引擎运行：

```powershell
docker version
docker compose version
```

Docker 仅用于本地启动 Redis 和 MinIO；MySQL、后端和前端也可以继续直接在主机运行。

### 4.7 推荐开发工具

- IntelliJ IDEA：打开 `backend`，运行 Spring Boot；
- VS Code：打开 `frontend/learning_platform_frontend`；
- MySQL Workbench 或 IDEA Database：执行数据库脚本；
- 浏览器开发者工具：检查 `/api` 请求与页面错误。

## 5. 首次初始化

所有命令默认在项目根目录 `codes` 执行。

### 5.1 启动 Redis 和 MinIO

复制基础设施环境变量模板：

```powershell
Copy-Item -LiteralPath 'infrastructure/.env.example' `
  -Destination 'infrastructure/.env'
```

编辑 `infrastructure/.env`，替换 Redis 密码和 MinIO 管理账号占位符。该文件已被
Git 忽略，不要把真实值写入 `.env.example`。

启动并检查容器：

```powershell
docker compose --env-file infrastructure/.env `
  -f infrastructure/docker-compose.local.yml up -d
docker compose --env-file infrastructure/.env `
  -f infrastructure/docker-compose.local.yml ps
```

浏览器访问 <http://127.0.0.1:9001> 登录 MinIO，创建私有存储桶
`learning-platform`。Redis 和 MinIO API 分别监听 `127.0.0.1:6379`、
`127.0.0.1:9000`。

### 5.2 初始化数据库

先以 MySQL 管理员连接并创建数据库与本地专用账号：

```sql
CREATE DATABASE IF NOT EXISTS `learning_platform`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'learning_platform'@'localhost'
    IDENTIFIED BY '<替换为本机强密码>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON `learning_platform`.* TO 'learning_platform'@'localhost';
FLUSH PRIVILEGES;
```

随后按顺序完整执行：

1. `database/001_schema.sql`
2. `database/002_seed_data.sql`

推荐使用 Workbench 的“打开 SQL 脚本”功能执行。也可进入 MySQL 命令行后执行：

```sql
SOURCE E:/你的项目路径/codes/database/001_schema.sql;
SOURCE E:/你的项目路径/codes/database/002_seed_data.sql;
```

路径使用 `/`。初始化后应有 33 张业务表，并包含 `USER`、`PUBLISHER`、`ADMIN`
三个角色。更详细的检查 SQL 见 [database/README.md](database/README.md)。

### 5.3 配置后端环境变量

变量清单见 [backend/.env.example](backend/.env.example)。Spring Boot 不会自动
读取普通 `.env` 文件，开发时推荐在 IDEA 中打开：

`Run` → `Edit Configurations` → Spring Boot → `Environment variables`

至少填写：

```text
APP_ENV=local
DB_URL=jdbc:mysql://localhost:3306/learning_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=learning_platform
DB_PASSWORD=<本机数据库密码>
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=<infrastructure/.env 中的 Redis 密码>
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=<本机 MinIO 用户名>
MINIO_SECRET_KEY=<本机 MinIO 密码>
MINIO_BUCKET=learning-platform
JWT_SECRET=<至少 32 字节的随机值>
AI_PROVIDER=mock
```

PowerShell 临时运行也可逐项设置 `$env:变量名='值'`，但只对当前终端生效。真实密码、
JWT Secret、MinIO Secret 和 AI Key 不得发给他人或提交到 Git。

Mock AI 不需要密钥。需要真实 AI 时再设置环境变量：

```text
AI_PROVIDER=deepseek
DEEPSEEK_API_KEY=<真实 API Key>
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-flash
```

### 5.4 启动后端

```powershell
Set-Location backend
mvn spring-boot:run
```

也可在 IDEA 运行 `LearningPlatformApplication`。启动成功后检查：

```text
http://localhost:8080/api/health
http://localhost:8080/actuator/health/readiness
```

前者应返回统一 API 格式且 `data.status` 为 `UP`，后者应返回
`{"status":"UP"}`。若 readiness 为 `DOWN`，优先检查 MySQL、Redis、MinIO 及存储桶。

### 5.5 安装依赖并启动前端

新开一个 PowerShell：

```powershell
Set-Location frontend/learning_platform_frontend
npm ci
npm run dev
```

浏览器访问 <http://localhost:5173>。默认不需要创建前端 `.env`；Vite 会把 `/api`
代理到 `http://localhost:8080`。需要修改后端地址时，复制 `.env.example` 为
`.env.local` 并调整 `VITE_API_PROXY_TARGET`。

`VITE_` 变量会进入浏览器构建产物，严禁在其中放任何服务端密码或 API Key。

## 6. 首个管理员与角色

初始化脚本故意不提供公开的默认管理员密码。推荐流程：

1. 在前端注册一个专用管理账号；
2. 在 MySQL 中确认用户名正确；
3. 通过下列 SQL 赋予管理员角色；
4. 重新登录，使前端重新获取当前用户信息。

```sql
INSERT IGNORE INTO user_role (user_id, role_id)
SELECT u.id, r.id
FROM user u
JOIN role r ON r.code = 'ADMIN'
WHERE u.username = '<你的管理员用户名>';
```

发布者角色应由管理员在管理后台分配。测试结束后不要保留弱密码或共享管理员账号。

## 7. 常用开发命令

### 后端

```powershell
Set-Location backend
mvn test
mvn clean package
mvn spring-boot:run
```

### 前端

```powershell
Set-Location frontend/learning_platform_frontend
npm ci
npm test
npm run build
npm run dev
```

生产构建输出位于 `frontend/learning_platform_frontend/dist`，后端 JAR 位于
`backend/target/learning-platform-backend-0.0.1-SNAPSHOT.jar`。

## 8. 环境变量分类

| 类别 | 关键变量 | 说明 |
| --- | --- | --- |
| 应用 | `SERVER_PORT`、`APP_ENV`、`APP_CORS_ALLOWED_ORIGINS` | 端口、环境和允许的前端来源 |
| MySQL | `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 使用专用低权限账号 |
| Redis | `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` | 登录防刷和接口状态 |
| MinIO | `MINIO_ENDPOINT`、`MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`MINIO_BUCKET` | 私有对象存储 |
| JWT | `JWT_SECRET`、`JWT_ACCESS_TOKEN_TTL_MINUTES` | 生产环境单独生成 |
| 上传 | `UPLOAD_MAX_FILE_SIZE`、`UPLOAD_MAX_FILES_PER_CONTENT` | 需与代理限制协调 |
| 限流 | `API_RATE_LIMIT_ENABLED` 及三类每分钟额度 | 生产环境保持开启 |
| AI | `AI_PROVIDER`、`DEEPSEEK_*`、`AI_*` | Key 只在后端配置 |
| 前端 | `VITE_APP_TITLE`、`VITE_API_BASE_URL` | 全部属于公开值 |

各变量的默认值和限制见 [环境配置说明.md](环境配置说明.md) 与示例文件。生产配置
方法见 [项目部署.md](项目部署.md)。

## 9. 常见问题

### 首页显示“后端暂未连接”

先直接访问 `http://localhost:8080/api/health`。若后端正常，再检查前端终端中的 Vite
代理错误和 `VITE_API_PROXY_TARGET`。修改 `.env.local` 后必须重启 Vite。

### 登录或注册显示“网络连接失败”

在浏览器 Network 中检查 `/api/auth/login` 或 `/api/auth/register`：

- `403`：检查是否绕过 Vite 代理、请求 Origin 和后端 CORS；
- `401`：清理浏览器旧 Token 后重试；
- 无响应：检查 8080 端口和后端控制台；
- `429`：等待 `Retry-After` 后重试，不要关闭生产限流。

### readiness 返回 DOWN

检查：

```powershell
Get-Service MySQL*
docker compose --env-file infrastructure/.env `
  -f infrastructure/docker-compose.local.yml ps
```

同时确认 Redis 密码、MinIO 凭证和 `learning-platform` 存储桶存在。

### 上传大文件失败

页面会显示各用途允许的格式和大小。浏览器收到 HTTP 413 时表示超过应用或反向代理
限制。开发环境应同时核对 `UPLOAD_MAX_FILE_SIZE` 和 Spring Multipart 的 200/220 MB
限制；部署环境还需设置 Nginx `client_max_body_size 220m`。

### AI 调用超时

确认 `AI_PROVIDER`、模型和 Key 配置正确。DeepSeek 模式下检查后端
`AI_PROVIDER_START/HEADERS/SUCCESS/FAILURE` 日志的 traceId，不要打印或发送 Key。
当前真实 AI 默认采用非流式 HTTP/1.1，超时层级为连接 10 秒、供应商 620 秒、任务
630 秒；反向代理读取超时必须大于任务超时。

### 端口被占用

```powershell
Get-NetTCPConnection -LocalPort 5173,8080,3306,6379,9000,9001 `
  -ErrorAction SilentlyContinue
```

修改端口时要同步修改调用方配置，不能只改其中一个服务。

## 10. 安全与数据说明

- 示例文件只能保存占位符，提交前执行 `git status` 检查 `.env`、证书和日志；
- MinIO 存储桶保持私有，下载和预览使用短期签名 URL；
- 操作日志默认保留 180 天；
- 模拟支付仅用于 MVP 验收；
- AI 生成内容需人工核验，不应输入无关敏感信息；
- 正式上线前补充运营主体、联系方式、第三方清单和隐私告知。

规则详见 [MVP 数据与内容治理规则](backend/docs/mvp-governance-policy.md) 和
[安全审查](backend/docs/security-review.md)。

## 11. 更多文档

- [详细部署指南](项目部署.md)
- [数据库初始化说明](database/README.md)
- [本地 Redis 与 MinIO](infrastructure/README.md)
- [管理员接口](backend/docs/admin-api.md)
- [AI 客户端](backend/docs/ai-client.md)
- [AI 学习接口](backend/docs/ai-learning-api.md)
- [安全与审计](backend/docs/security-and-audit.md)
- [已知非阻断限制](backend/docs/known-issues.md)

