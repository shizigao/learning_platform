/**
 * 线下教学模块。
 *
 * <p>负责教师申请、管理员审核、公开教师档案、可授课时间、搜索和 AI 推荐。
 * 推荐先使用地区、预算、内容与时间等本地规则筛选候选人，再把至多二十名候选人
 * 交给 AI 排序；身份证号等敏感申请字段不得出现在公开 DTO 或推荐提示词中。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/OfflineStudentPreference.java}：表示线下教学Student学习需求领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/OfflineTeacherApplication.java}：表示线下教学教师申请领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/OfflineTeacherProfile.java}：表示线下教学教师资料领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/OfflineTeacherRecommendation.java}：表示线下教学教师推荐领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/TeacherApplicationAdminView.java}：表示教师申请管理浏览领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/TeacherApplicationStatus.java}：枚举教师申请状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/TeacherProfileStatus.java}：枚举教师资料状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code dto/OfflineTeachingDtos.java}：表示线下教学教学Dtos领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/TeacherApplicationAdminQuery.java}：定义教师申请管理查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/TeacherSearchQuery.java}：定义教师搜索查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code mapper/OfflineTeachingMapper.java}：定义线下教学教学的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code security/TeacherSensitiveDataCrypto.java}：表示教师敏感配置DataCrypto领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code service/OfflineTeacherService.java}：实现线下教学教师业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/TeacherRecommendationPersistenceService.java}：实现教师推荐持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/TeacherRecommendationService.java}：实现教师推荐业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/AdminOfflineTeachingController.java}：提供管理线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/OfflineTeachingController.java}：提供线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.offline;
