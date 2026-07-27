# 通用组件

- `AppLogo`：平台标识。
- `SectionPageHeader`：业务页面统一标题区。
- `ContentCard`、`ContentStatusTag`：资料卡片与状态展示。
- `MarkdownEditor`、`MarkdownRenderer`：Markdown 编辑、预览、资源引用解析与安全渲染。
- `CategoryPickerDialog`：可搜索的大量资料分类选择器。
- `ContentReferencePickerDialog`：按资料标题和发布者分别搜索资料引用。
- `ClassPickerDialog`：从当前用户可管理班级中选择发放范围。
- `UserSearchPanel`：用户搜索与个人主页入口。
- `PublisherExamNav`：发布者考试相关页面的局部导航。
- `ThumbUpIcon`：统一点赞图标。

组件通过 `props` 接收数据，通过 `emit` 汇报选择或操作，不直接修改页面状态。只有承担完整数据检索职责的选择器组件可以调用 API。
