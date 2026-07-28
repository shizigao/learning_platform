/**
 * 考试编排与作答模块。
 *
 * <p>负责试卷快照、考试创建发布、参与范围、考生会话、答案暂存和交卷。
 * 发布后的试卷与题目采用快照，避免题库后续修改影响历史考试；交卷与超时处理
 * 必须保持幂等。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/Exam.java}：表示考试领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamAnswer.java}：表示考试答案领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamAnswerGradingStatus.java}：枚举考试答案阅卷状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamAssignmentMode.java}：枚举考试Assignment模式允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamAttempt.java}：表示考试作答领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamAttemptStatus.java}：枚举考试作答状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamCandidate.java}：表示考试考生领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamCandidateStatus.java}：枚举考试考生状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamPaper.java}：表示考试试卷领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamPaperQuestion.java}：表示考试试卷题目领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamPaperStatus.java}：枚举考试试卷状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamQuestionStatistics.java}：表示考试题目Statistics领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamResult.java}：表示考试成绩领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamStatisticsSummary.java}：表示考试Statistics总结领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamStatus.java}：枚举考试状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamSubmissionType.java}：枚举考试交卷类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/WrongReviewExam.java}：表示错题复习考试领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/CandidateExamOverviewResponse.java}：定义考生考试Overview响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/CandidateExamResponse.java}：定义考生考试响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/CandidatePaperQuestionResponse.java}：定义考生试卷题目响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamAnswerBatchItemRequest.java}：定义考试答案BatchItem请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ExamAnswerBatchSaveRequest.java}：定义考试答案Batch保存请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ExamAnswerResponse.java}：定义考试答案响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamAnswerWriteRequest.java}：定义考试答案Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ExamCandidateOptionResponse.java}：定义考试考生选项响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamCandidateResponse.java}：定义考试考生响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamCandidateSearchQuery.java}：定义考试考生搜索查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ExamEligibilityResponse.java}：定义考试Eligibility响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamGradingAttemptResponse.java}：定义考试阅卷作答响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamGradingDetailResponse.java}：定义考试阅卷详情响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamListQuery.java}：定义考试列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ExamManagementResponse.java}：定义考试Management响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamPaperDetailResponse.java}：定义考试试卷详情响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamPaperListQuery.java}：定义考试试卷列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ExamPaperSummaryResponse.java}：定义考试试卷总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamPaperWriteRequest.java}：定义考试试卷Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ExamQuestionStatisticsResponse.java}：定义考试题目Statistics响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamResultDetailResponse.java}：定义考试成绩详情响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamResultQuestionResponse.java}：定义考试成绩题目响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamResultSummaryResponse.java}：定义考试成绩总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamStartResponse.java}：定义考试开始响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamStatisticsResponse.java}：定义考试Statistics响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamSubmissionResponse.java}：定义考试交卷响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamSummaryResponse.java}：定义考试总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamWriteRequest.java}：定义考试Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ManualGradeRequest.java}：定义Manual评分请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/PaperQuestionManagementResponse.java}：定义试卷题目Management响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/PaperQuestionWriteRequest.java}：定义试卷题目Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ReplacePaperQuestionsRequest.java}：定义替换试卷Questions请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/WrongReviewExamResponse.java}：定义错题复习考试响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code mapper/ExamAnswerMapper.java}：定义考试答案的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamAttemptMapper.java}：定义考试作答的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamCandidateMapper.java}：定义考试考生的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamMapper.java}：定义考试的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamPaperMapper.java}：定义考试试卷的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamPaperQuestionMapper.java}：定义考试试卷题目的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamResultMapper.java}：定义考试成绩的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamStatisticsMapper.java}：定义考试Statistics的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/CandidateExamSessionService.java}：实现考生考试Session业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamAnswerPresentationService.java}：实现考试答案Presentation业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamAnswerService.java}：实现考试答案业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamGradingService.java}：实现考试阅卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamPaperService.java}：实现考试试卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamPublishQuotaService.java}：实现考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamResultService.java}：实现考试成绩业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamRuntimeStateService.java}：实现考试运行态State业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamService.java}：实现考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamStatisticsService.java}：实现考试Statistics业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamSubmissionService.java}：实现考试交卷业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamTimeoutScheduler.java}：按配置周期调度考试超时调度器，并把实际业务处理委托给服务层。</li>
 *   <li>{@code web/CandidateExamController.java}：提供考生考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/PublisherExamCandidateController.java}：提供发布者考试考生相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/PublisherExamController.java}：提供发布者考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/PublisherExamGradingController.java}：提供发布者考试阅卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/PublisherExamPaperController.java}：提供发布者考试试卷相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/WrongQuestionReviewController.java}：提供错题题目复习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.exam;
