# Learning Platform Backend

智能在线学习考试平台后端，采用 Spring Boot 3.5、Spring Security、MyBatis、MySQL、Redis、MinIO 和 JWT。

## 本地开发

1. 使用 IDEA 打开本目录并选择 JDK 21。
2. Maven 编译目标为 Java 17。
3. 参照 `.env.example` 在 IDEA 运行配置中设置环境变量。
4. 启动 `LearningPlatformApplication`。

## 常用命令

```powershell
mvn test
mvn spring-boot:run
```

默认健康检查：`GET http://localhost:8080/api/health`。

完整的环境搭建、数据库及基础设施初始化见项目根目录
[`README.md`](../README.md)，生产发布见
[`项目部署.md`](../项目部署.md)。
