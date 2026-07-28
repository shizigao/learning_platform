/**
 * 用户与个人中心模块。
 *
 * <p>负责账户资料、角色关联、公开个人主页、头像、用户搜索及发布统计聚合。
 * 身份凭证由认证模块管理，头像文件通过内容存储基础设施进入 MinIO。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/Role.java}：表示角色领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/RoleCode.java}：枚举角色编码允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/User.java}：表示用户领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/UserAvatar.java}：表示用户头像领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/UserRole.java}：表示用户角色领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/UserStatus.java}：枚举用户状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code dto/AvatarUploadResponse.java}：定义头像上传响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/PublicUserProfileResponse.java}：定义Public用户资料响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/PublicUserSummaryResponse.java}：定义Public用户总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/UserPublicationStatsResponse.java}：定义用户PublicationStats响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/UserSearchQuery.java}：定义用户搜索查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code mapper/RoleMapper.java}：定义角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/UserAvatarMapper.java}：定义用户头像的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/UserMapper.java}：定义用户的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/UserRoleMapper.java}：定义用户角色的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/PublicUserProfileService.java}：实现Public用户资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/RoleService.java}：实现角色业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/UserAvatarService.java}：实现用户头像业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/UserService.java}：实现用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/UserProfileController.java}：提供用户资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.user;
