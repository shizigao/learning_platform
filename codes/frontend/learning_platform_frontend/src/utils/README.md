# 工具模块

- `auth-storage.ts`：Token 及本地过期时间的读写。
- `markdown.ts`：Markdown 转换、XSS 清洗和平台资源协议解析。
- `exam-answer.ts`：不同题型的草稿到提交载荷转换。
- `reading-progress.ts`：正文滚动位置到 0–100 学习进度的换算。
- `upload-rules.ts`：不同文件角色的扩展名、大小和提示文案。

工具函数应尽量保持纯函数。涉及浏览器存储的函数必须在函数注释中说明副作用；安全相关转换不能由调用方跳过。
