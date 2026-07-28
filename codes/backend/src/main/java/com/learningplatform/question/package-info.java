/**
 * 题库模块。
 *
 * <p>负责题库、题目、选项、标准答案和解析。不同题型拥有不同答案结构，
 * 服务层在入库前统一规范化并校验；被试卷快照引用后的历史数据不依赖题库现状。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/Question.java}：表示题目领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/QuestionBank.java}：表示题目题库领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/QuestionOption.java}：表示题目选项领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/QuestionStatus.java}：枚举题目状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/QuestionType.java}：枚举题目类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code dto/CandidateQuestionResponse.java}：定义考生题目响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/QuestionAnswer.java}：以不可变记录表示题目答案数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code dto/QuestionBankResponse.java}：定义题目题库响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/QuestionBankWriteRequest.java}：定义题目题库Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/QuestionListQuery.java}：定义题目列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/QuestionManagementResponse.java}：定义题目Management响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/QuestionOptionResponse.java}：定义题目选项响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/QuestionOptionWriteRequest.java}：定义题目选项Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/QuestionWriteRequest.java}：定义题目Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code mapper/QuestionBankMapper.java}：定义题目题库的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/QuestionMapper.java}：定义题目的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/QuestionOptionMapper.java}：定义题目选项的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/QuestionBankService.java}：实现题目题库业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/QuestionService.java}：实现题目业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/PublisherQuestionController.java}：提供发布者题目相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.question;
