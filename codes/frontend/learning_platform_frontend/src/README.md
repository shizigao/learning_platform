# 前端源码导读

前端采用 Vue 3 + TypeScript + Pinia + Vue Router。推荐按照“路由 → 页面 → API → 类型”的顺序阅读：

- `router/`：页面入口、登录校验和角色权限。
- `views/`：页面级组件，负责组织数据加载、表单状态和用户操作。
- `components/`：可跨页面复用的展示或交互组件。
- `api/`：后端接口适配；页面不应直接使用 Axios。
- `types/`：接口请求、响应和领域枚举的 TypeScript 定义。
- `stores/`：跨页面共享的会话与应用状态。
- `utils/`：无页面依赖的格式转换、校验和本地存储逻辑。
- `layouts/`：公共导航、页脚以及登录页框架。
- `assets/`：静态资源；业务数据中的图片应使用后端文件地址。

## 数据流

```text
用户操作 → View → api/*.ts → http.ts → Spring Boot
            ↓         ↓
         Pinia     types/*.ts
```

页面负责“何时请求”和“如何展示”；API 层负责“请求哪个接口”和“如何解包”；`http.ts` 统一负责 Token、请求追踪、超时和异常规范化。

## 扩展约定

新增业务能力时，先补充 `types`，再增加 `api` 方法，最后在 `views` 中编排。需要复用的交互抽到 `components`，纯转换规则抽到 `utils`。新页面必须在路由元信息中明确 `requiresAuth` 与 `roles`。
