# 页面模块

页面按用户旅程分组：

- 公共与账户：`HomeView`、`LoginView`、`RegisterView`、`ProfileView`、`PublicUserProfileView`。
- 学习资料：`ContentsView`、`ContentDetailView`、`LearningView`、`MyLearningView`、`FavoritesView`。
- 考试参与：`ExamsView`、`ExamEntryView`、`ExamResultView`、`WrongQuestionReviewView`。
- AI：`AiAssistantView`、`ExamAiAnalysisView`。
- 发布工作台：`PublisherContentsView`、`PublisherContentEditorView`、`PublisherQuestionsView`、`PublisherPapersView`、`PublisherExamsView`、`PublisherExamGradingView`。
- 班级与线下教学：`MyClassesView`、`ClassManagementView`、`OfflineTeachingView`。
- 商业化：`CommerceView`。
- 管理：`AdminWorkspaceView`、`AdminAiConfigView`、`AdminOfflineTeachersView`。
- 兜底：`ErrorView`、`NotFoundView`。

页面组件负责加载态、空态、错误态、提交互斥和成功反馈。长耗时 AI 请求必须显示明确阶段，禁止在请求未结束时重复提交。
