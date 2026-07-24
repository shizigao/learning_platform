# 数据库初始化操作说明

## 一、前置条件

- MySQL 8.0.x 已启动。
- 使用管理员账号连接 MySQL。
- 真实数据库密码由你自行设置，不写入项目文件或聊天消息。

## 二、创建数据库和专用账号

在 MySQL Workbench、IDEA Database 或其他数据库工具中新建查询窗口，将下面的密码占位符替换为你自己的强密码后执行：

```sql
CREATE DATABASE IF NOT EXISTS `learning_platform`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'learning_platform'@'localhost'
    IDENTIFIED BY '<请替换为本机开发数据库强密码>';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON `learning_platform`.* TO 'learning_platform'@'localhost';

FLUSH PRIVILEGES;
```

不要把替换过密码的 SQL 保存到项目目录。若应用通过 Docker 或其他主机连接，应根据网络环境另外创建受限来源账号，不要直接使用 `'%'` 放开所有来源。

## 三、执行脚本

依次执行：

1. `001_schema.sql`
2. `002_seed_data.sql`

两个脚本都先执行 `USE learning_platform`。`001_schema.sql` 只使用 `CREATE TABLE IF NOT EXISTS`，不会删除现有数据；如在已有结构上执行，必须先让 AI 生成增量迁移脚本，不能依靠 `IF NOT EXISTS` 自动更新旧表。

### 遇到过“Specified key was too long”时

早期脚本曾直接对 MinIO 的长对象名建立联合唯一索引，在 `utf8mb4` 下会超过 InnoDB 的 3072 字节索引上限。当前脚本已改为对自动生成的 SHA-256 二进制列建立唯一索引。

如果错误发生在首次执行 `001_schema.sql` 的过程中：

1. 不要删除已成功创建的表。
2. 重新打开最新的 `001_schema.sql`，从头完整执行一次。
3. 已存在的表会被 `IF NOT EXISTS` 跳过，失败的 `content_file` 表及其后续表会继续创建。
4. `001_schema.sql` 成功后再执行 `002_seed_data.sql`。

## 四、执行后检查

```sql
USE `learning_platform`;

SELECT COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'learning_platform';

SELECT id, code, name FROM role ORDER BY id;
SELECT id, product_code, product_type, name, price FROM product ORDER BY id;
SELECT config_key, config_value, value_type FROM system_config ORDER BY config_key;
```

预期结果：

- 业务表共 33 张。
- 角色包含 `USER`、`PUBLISHER`、`ADMIN`。
- 初始化商品包含 AI 次数包和考试发布次数包。
- `system_config` 不包含数据库密码、MinIO 密钥或 DeepSeek API Key。

## 五、IDEA 环境变量

数据库和账号创建成功后，在 IDEA 后端运行配置中设置：

```text
DB_URL=jdbc:mysql://localhost:3306/learning_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=learning_platform
DB_PASSWORD=<你的本机数据库密码>
```

密码只填写在本机 IDEA 运行配置中，不要发给 AI。

## 六、管理员初始化方案

当前阶段只初始化角色，不写入带默认密码的管理员，避免仓库内出现公开管理员凭证。

认证模块完成后按以下安全流程创建管理员：

1. 由后端 BCrypt 密码编码器生成哈希。
2. 你在本机选择管理员用户名和初始强密码。
3. AI 提供只含密码哈希、不含明文密码的本地初始化命令或一次性管理命令。
4. 创建管理员后立即登录并修改初始密码。
5. 通过 `user_role` 关联 `ADMIN` 角色。

在认证模块完成前，不要自行向 `user.password_hash` 写入明文密码。
