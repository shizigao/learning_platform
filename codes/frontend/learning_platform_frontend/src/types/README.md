# 类型模块

本目录按后端领域保存 TypeScript 类型，字段名与 JSON 契约保持一致。

- `api.ts`：统一响应壳。
- `auth.ts`、`user.ts`：身份、角色和用户资料。
- `content.ts`：资料、分类、文件、互动和发布流程。
- `exam.ts`：题目、试卷、考试、作答、成绩和阅卷。
- `ai.ts`：AI 任务、会话、总结和分析结果。
- `classroom.ts`：班级、成员、公告和范围。
- `order.ts`：商品、订单和权益。
- `offline-teaching.ts`：教师申请、档案和推荐。
- `admin.ts`：治理与审计。

接口字段变化时应先修改这里，让 TypeScript 编译器指出所有受影响调用点；不要在页面中使用 `any` 绕过契约。
