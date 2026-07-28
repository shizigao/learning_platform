/**
 * 学习资料模块。
 *
 * <p>由分类、资料元数据、Markdown 正文、文件、站内资料引用、发布审核和访问控制
 * 构成。文件实际内容保存在 MinIO，数据库保存对象名及元数据；公开、付费和班级
 * 发放模式统一通过 {@code ContentAccessService} 判定访问权。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/ContentCategory.java}：表示学习资料分类领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ContentDistributionMode.java}：枚举学习资料发放模式允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ContentFile.java}：表示学习资料文件领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ContentFileRole.java}：枚举学习资料文件角色允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ContentFileStatus.java}：枚举学习资料文件状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ContentPublicationStats.java}：表示学习资料PublicationStats领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ContentStatus.java}：枚举学习资料状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ContentType.java}：枚举学习资料类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/LearningContent.java}：表示学习资料领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/AdminContentListQuery.java}：定义管理学习资料列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/CategoryWriteRequest.java}：定义分类Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/ContentCategoryResponse.java}：定义学习资料分类响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ContentCategorySearchQuery.java}：定义学习资料分类搜索查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ContentDetailResponse.java}：定义学习资料详情响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ContentFileResponse.java}：定义学习资料文件响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ContentListQuery.java}：定义学习资料列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ContentReferenceSearchQuery.java}：定义学习资料Reference搜索查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/ContentSummaryResponse.java}：定义学习资料总结响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ContentWriteRequest.java}：定义学习资料Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/FileUrlResponse.java}：定义文件Url响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/PublisherContentListQuery.java}：定义发布者学习资料列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/RejectContentRequest.java}：定义驳回学习资料请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code mapper/ContentCategoryMapper.java}：定义学习资料分类的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ContentFileMapper.java}：定义学习资料文件的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/LearningContentMapper.java}：定义学习资料的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/ContentAccessService.java}：实现学习资料访问权业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ContentCategoryService.java}：实现学习资料分类业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ContentFileService.java}：实现学习资料文件业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/LearningContentService.java}：实现学习资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code storage/FileContentSignatureValidator.java}：校验文件学习资料Signature校验器的格式和业务不变量，失败时返回明确错误。</li>
 *   <li>{@code storage/FileUploadValidationRequest.java}：定义文件上传Validation请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code storage/FileUploadValidator.java}：校验文件上传校验器的格式和业务不变量，失败时返回明确错误。</li>
 *   <li>{@code storage/MinioStorageService.java}：实现Minio存储业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code storage/StorageObjectKeyFactory.java}：集中创建存储Object键工厂，保证不同调用场景使用一致规则。</li>
 *   <li>{@code storage/StorageUploadRequest.java}：定义存储上传请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code storage/StoredObject.java}：以不可变记录表示StoredObject数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code storage/ValidatedUploadFile.java}：以不可变记录表示Validated上传文件数据，并作为模块内部或接口层的数据契约。</li>
 *   <li>{@code web/AdminContentController.java}：提供管理学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/ContentQueryController.java}：提供学习资料查询条件相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/PublisherContentController.java}：提供发布者学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.content;
