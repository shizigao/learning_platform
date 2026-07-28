/**
 * AI 能力模块。
 *
 * <p>包含供应商客户端、提示词构造、资料总结、知识讲解、考试分析、错题分析、
 * 教师推荐、任务生命周期和调用用量。长请求通过请求幂等号、任务状态和调用守卫
 * 防止重复扣费；业务服务只接收供应商无关的 {@code AiClient} 接口。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code client/AiClient.java}：定义或实现AI 客户端外部调用适配，隔离供应商协议与业务服务。</li>
 *   <li>{@code client/AiClientException.java}：表示AI 客户端异常失败，携带可由统一异常处理器转换的错误语义。</li>
 *   <li>{@code client/AiClientRequest.java}：定义AI 客户端请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code client/AiClientResponse.java}：定义AI 客户端响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code client/AiMessage.java}：以不可变记录表示AI消息数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code client/AiResponseFormat.java}：枚举AI响应Format允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code client/AiRole.java}：枚举AI角色允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code client/DeepSeekAiClient.java}：定义或实现DeepSeek AI 客户端外部调用适配，隔离供应商协议与业务服务。</li>
 *   <li>{@code client/MockAiClient.java}：定义或实现模拟AI 客户端外部调用适配，隔离供应商协议与业务服务。</li>
 *   <li>{@code config/AiClientConfiguration.java}：装配AI 客户端配置运行配置和依赖组件，并对关键配置项执行启动期校验。</li>
 *   <li>{@code domain/AiConversation.java}：表示AI会话领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/AiConversationStatus.java}：枚举AI会话状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/AiConversationTemplate.java}：枚举AI会话模板允许的有限取值，供持久化、校验和状态分支共同使用。</li>
 *   <li>{@code domain/AiMessage.java}：表示AI消息领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/AiMessageRole.java}：枚举AI消息角色允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/AiSummary.java}：表示AI总结领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/AiTask.java}：表示AI任务领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/AiTaskStatus.java}：枚举AI任务状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/AiTaskType.java}：枚举AI任务类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/AiUsageRecord.java}：表示AI用量记录领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/AiUsageStatus.java}：枚举AI用量状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ExamAiAnalysis.java}：表示考试AI分析领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ExamAiAnalysisScope.java}：枚举考试AI分析范围允许的有限取值，供持久化、校验和状态分支共同使用。</li>
 *   <li>{@code domain/WrongQuestionAnalysis.java}：表示错题题目分析领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/AdminAiConfigResponse.java}：定义管理AI配置响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiConversationCreateRequest.java}：定义AI会话创建请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/AiConversationResponse.java}：定义AI会话响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiExplanationRequest.java}：定义AI讲解请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/AiExplanationResponse.java}：定义AI讲解响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiMessageResponse.java}：定义AI消息响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiSummaryGenerateRequest.java}：定义AI总结生成请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/AiSummaryResponse.java}：定义AI总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiTaskResponse.java}：定义AI任务响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AiTemplateRequest.java}：定义AI模板请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/AiUsageRecordResponse.java}：定义AI用量记录响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamAiAnalysisGenerateRequest.java}：定义考试AI分析生成请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ExamAiAnalysisPageResponse.java}：定义考试AI分析分页响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ExamAiAnalysisResponse.java}：定义考试AI分析响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/WrongQuestionAnalysisGenerateRequest.java}：定义错题题目分析生成请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/WrongQuestionAnalysisResponse.java}：定义错题题目分析响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/WrongQuestionReviewPageResponse.java}：定义错题题目复习分页响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code mapper/AiConversationMapper.java}：定义AI会话的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/AiMessageMapper.java}：定义AI消息的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/AiSummaryMapper.java}：定义AI总结的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/AiTaskMapper.java}：定义AI任务的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/AiUsageRecordMapper.java}：定义AI用量记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ExamAiAnalysisMapper.java}：定义考试AI分析的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/WrongQuestionAnalysisMapper.java}：定义错题题目分析的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/AdminAiConfigService.java}：实现管理AI配置业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AiConversationPromptFactory.java}：集中创建AI会话提示词工厂，保证不同调用场景使用一致规则。</li>
 *   <li>{@code service/AiConversationService.java}：实现AI会话业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AiQuotaService.java}：实现AI额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AiRequestGuard.java}：保护AI请求保护调用的频率、并发和超时边界，并把底层失败转换为安全错误。</li>
 *   <li>{@code service/AiResultPersistenceService.java}：实现AI成绩持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AiSummaryService.java}：实现AI总结业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AiTaskLifecycleService.java}：实现AI任务生命周期业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamAiAnalysisPersistenceService.java}：实现考试AI分析持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ExamAiAnalysisService.java}：实现考试AI分析业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/WrongQuestionAnalysisPersistenceService.java}：实现错题题目分析持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/WrongQuestionReviewService.java}：实现错题题目复习业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code text/ContentTextExtractor.java}：从输入中提取并规范化学习资料Text提取器，为后续业务或 AI 调用提供安全文本。</li>
 *   <li>{@code text/ExtractedContentText.java}：以不可变记录表示Extracted学习资料Text数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code web/AdminAiController.java}：提供管理AI相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/AiLearningController.java}：提供AI学习相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.ai;
