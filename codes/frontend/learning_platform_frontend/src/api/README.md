# API 模块

本目录是浏览器与后端之间的唯一网络访问层。

- `http.ts`：Axios 实例、Token 注入、`X-Request-Id`、统一错误转换和 401 会话清理。
- `auth.ts`：注册、登录、登出和当前用户资料。
- `user.ts`：公开个人主页、头像、用户搜索。
- `content.ts`：学习资料、分类、文件、互动、学习记录和发布者工作台。
- `exam.ts`：题库、试卷、考试、作答、阅卷、统计和错题复习。
- `ai.ts`：资料总结、讲解会话、考试分析、错题分析及任务状态。
- `classroom.ts`：班级、成员、公告与班级资源。
- `order.ts`：商品、订单、模拟支付和权益。
- `offline-teaching.ts`：教师申请、检索与 AI 推荐。
- `admin.ts`：管理员用户、内容、订单、权益和审计操作。
- `health.ts`：后端就绪状态探测。

接口函数应返回已解包的业务数据，不向页面暴露 Axios 响应对象。所有失败都由 `http.ts` 转换为 `ApiError`；页面可以使用 `status`、`code` 和 `traceId` 做针对性提示。
