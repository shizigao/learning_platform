# 智学云考前端

Vue 3、TypeScript、Vite 和 Element Plus 实现的前端应用。

```powershell
npm ci
npm test
npm run dev
npm run build
```

开发服务器默认监听 5173，并将 `/api` 代理到 `http://localhost:8080`。公开环境变量
见 `.env.example`；任何 `VITE_` 变量都会进入浏览器构建产物，不得保存服务端密钥。

完整的环境搭建、数据库与基础设施初始化请阅读项目根目录
[`README.md`](../../README.md)，生产发布请阅读
[`项目部署.md`](../../项目部署.md)。
