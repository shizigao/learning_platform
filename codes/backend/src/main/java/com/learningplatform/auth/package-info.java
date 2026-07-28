/**
 * 身份认证模块。
 *
 * <p>负责注册、登录、退出、JWT 生成与校验、当前用户解析和 Spring Security
 * 过滤链。认证只回答“请求者是谁”，具体资料、考试、班级等资源权限由相应领域
 * 服务判断。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code dto/LoginRequest.java}：定义Login请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/LoginResponse.java}：定义Login响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/RegisterRequest.java}：定义Register请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/UpdateProfileRequest.java}：定义更新资料请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/UserProfileResponse.java}：定义用户资料响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code security/ApiAccessDeniedHandler.java}：统一处理Api访问权Denied处理器场景并转换为平台约定的结果。</li>
 *   <li>{@code security/ApiAuthenticationEntryPoint.java}：表示Api认证EntryPoint领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code security/AuthenticatedUserPrincipal.java}：以不可变记录表示Authenticated用户认证主体数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code security/AuthenticationPrincipalResolver.java}：集中解析认证认证主体解析器，避免各调用方重复实现协议或身份转换逻辑。</li>
 *   <li>{@code security/JwtAuthenticationFilter.java}：在 Servlet 过滤链中处理JWT认证过滤器，并在请求进入 Controller 前建立安全或上下文约束。</li>
 *   <li>{@code security/JwtTokenClaims.java}：以不可变记录表示JWT令牌Claims数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code security/JwtTokenService.java}：实现JWT令牌业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code security/LoginProtectionService.java}：实现LoginProtection业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code security/OwnerOrAdmin.java}：定义OwnerOr管理注解，用于以声明式方式标记相关行为。</li>
 *   <li>{@code security/ResourceAuthorization.java}：表示Resource授权领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code service/AuthService.java}：实现认证业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/AuthController.java}：提供认证相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.auth;
