/**
 * 班级模块。
 *
 * <p>负责班级邀请码、成员关系、拥有者/管理员/成员角色、公告以及班级资料和考试
 * 范围。拥有者具有完整管理权，管理员只能执行授权操作；离开、移除和解散操作
 * 必须维护班级至少一个拥有者的不变量。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/ClassAnnouncement.java}：表示班级公告领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ClassMember.java}：表示班级成员领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ClassMemberStatus.java}：枚举班级成员状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ClassMemberView.java}：表示班级成员浏览领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ClassRole.java}：枚举班级角色允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ClassStatus.java}：枚举班级状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/LearningClass.java}：表示学习班级领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/AnnouncementResponse.java}：定义公告响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/AnnouncementWriteRequest.java}：定义公告Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ClassMemberListQuery.java}：定义班级成员列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ClassMemberResponse.java}：定义班级成员响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ClassMemberRoleRequest.java}：定义班级成员角色请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ClassSummaryResponse.java}：定义班级总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ClassWriteRequest.java}：定义班级Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/InviteEnabledRequest.java}：定义邀请码启用状态请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/JoinClassRequest.java}：定义Join班级请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/TransferOwnershipRequest.java}：定义TransferOwnership请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code mapper/ClassroomMapper.java}：定义班级的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ClassScopeMapper.java}：定义班级范围的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/ClassAnnouncementService.java}：实现班级公告业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ClassroomService.java}：实现班级业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/ClassController.java}：提供班级相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/ClassManagementController.java}：提供班级Management相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.classroom;
