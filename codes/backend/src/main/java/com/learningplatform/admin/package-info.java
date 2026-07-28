/**
 * 平台治理模块。
 *
 * <p>由管理员接口、治理服务、审计日志和运行配置构成，负责用户状态与角色、
 * 资料审核、订单查询、权益调整、教师申请审核以及 AI 运行参数管理。
 * 管理操作必须记录操作者、目标、变更摘要和 traceId，不能绕过领域服务直接改表。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code audit/OperationAuditFilter.java}：在 Servlet 过滤链中处理操作审核过滤器，并在请求进入 Controller 前建立安全或上下文约束。</li>
 *   <li>{@code audit/OperationLog.java}：表示操作日志领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code audit/OperationLogMapper.java}：定义操作日志的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code audit/OperationLogService.java}：实现操作日志业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code audit/OperationResult.java}：枚举操作成绩允许的有限取值，供持久化、校验和状态分支共同使用。</li>
 *   <li>{@code dto/AdminExamDetailResponse.java}：定义管理考试详情响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AdminExamListQuery.java}：定义管理考试列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/AdminExamSummaryResponse.java}：定义管理考试总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AdminUserListQuery.java}：定义管理用户列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/AdminUserResponse.java}：定义管理用户响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AdminUserRolesRequest.java}：定义管理用户Roles请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/AdminUserStatusRequest.java}：定义管理用户状态请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/OperationLogListQuery.java}：定义操作日志列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/OperationLogResponse.java}：定义操作日志响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/AdminExamService.java}：实现管理考试业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/AdminUserService.java}：实现管理用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/AdminExamController.java}：提供管理考试相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/AdminUserController.java}：提供管理用户相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/OperationLogController.java}：提供操作日志相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.admin;
