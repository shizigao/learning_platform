/**
 * 跨领域基础设施。
 *
 * <p>包含统一响应与错误码、全局异常处理、请求 traceId、分页模型和通用配置。
 * 本包不得依赖具体业务领域，业务模块可以单向依赖本包。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code api/ApiResponse.java}：定义Api响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code api/ErrorCode.java}：枚举错误编码允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code config/AiProperties.java}：承载AI配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/ApiRateLimitProperties.java}：承载Api频率限制配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/ConnectionHealthConfig.java}：装配Connection健康检查配置运行配置和依赖组件，并对关键配置项执行启动期校验。</li>
 *   <li>{@code config/CorsProperties.java}：承载Cors配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/JwtProperties.java}：承载JWT配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/LoginProtectionProperties.java}：承载LoginProtection配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/MinioProperties.java}：承载Minio配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/SecurityConfig.java}：装配安全配置运行配置和依赖组件，并对关键配置项执行启动期校验。</li>
 *   <li>{@code config/TeacherDataSecurityProperties.java}：承载教师Data安全配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code config/UploadProperties.java}：承载上传配置属性配置属性，供配置装配和业务组件以类型安全方式读取。</li>
 *   <li>{@code exception/BusinessException.java}：表示Business异常失败，携带可由统一异常处理器转换的错误语义。</li>
 *   <li>{@code exception/GlobalExceptionHandler.java}：统一处理Global异常处理器场景并转换为平台约定的结果。</li>
 *   <li>{@code model/BaseEntity.java}：表示BaseEntity领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code page/PageQuery.java}：定义分页查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code page/PageResult.java}：以不可变记录表示分页成绩数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code web/ApiRateLimitFilter.java}：在 Servlet 过滤链中处理Api频率限制过滤器，并在请求进入 Controller 前建立安全或上下文约束。</li>
 *   <li>{@code web/HealthController.java}：提供健康检查相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/TraceIdFilter.java}：在 Servlet 过滤链中处理链路追踪ID过滤器，并在请求进入 Controller 前建立安全或上下文约束。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.common;
