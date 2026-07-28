/**
 * 商业化与权益模块。
 *
 * <p>负责商品、订单、订单项、模拟支付及资料访问权/次数权益。订单保存商品快照，
 * 支付后的权益发放按订单项幂等；次数扣减通过行锁和乐观版本避免并发透支。</p>
 
 * <!-- FILE_INDEX_START -->
 * <h2>文件职责索引</h2>
 * <ul>
 *   <li>{@code domain/EntitlementStatus.java}：枚举权益状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/EntitlementType.java}：枚举权益类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/Order.java}：表示订单领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/OrderItem.java}：表示订单Item领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/OrderStatus.java}：枚举订单状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/PaymentProvider.java}：枚举支付供应商允许的有限取值，供持久化、校验和状态分支共同使用。</li>
 *   <li>{@code domain/PaymentRecord.java}：表示支付记录领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/PaymentStatus.java}：枚举支付状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/Product.java}：表示商品领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code domain/ProductStatus.java}：枚举商品状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/ProductType.java}：枚举商品类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。</li>
 *   <li>{@code domain/UserEntitlement.java}：表示用户权益领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code dto/AdminOrderListQuery.java}：定义管理订单列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/EntitlementBalancesResponse.java}：定义权益Balances响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/EntitlementResponse.java}：定义权益响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/MockPaymentResponse.java}：定义模拟支付响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/OrderCreateItemRequest.java}：定义订单创建Item请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/OrderCreateRequest.java}：定义订单创建请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code dto/OrderItemResponse.java}：定义订单Item响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/OrderListQuery.java}：定义订单列表查询条件列表或检索接口的查询条件、分页参数和默认值。</li>
 *   <li>{@code dto/OrderResponse.java}：定义订单响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/PaymentRecordResponse.java}：定义支付记录响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ProductResponse.java}：定义商品响应接口的只读返回契约，避免直接暴露数据库实体。</li>
 *   <li>{@code dto/ProductWriteRequest.java}：定义商品Write请求接口的请求字段和 Bean Validation 约束。</li>
 *   <li>{@code mapper/OrderItemMapper.java}：定义订单Item的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/OrderMapper.java}：定义订单的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/PaymentRecordMapper.java}：定义支付记录的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/ProductMapper.java}：定义商品的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code mapper/UserEntitlementMapper.java}：定义用户权益的 MyBatis 查询和写入操作，是业务服务访问数据库的持久化端口。</li>
 *   <li>{@code package-info.java}：说明本模块总体职责、分层边界以及全部源码文件的用途。</li>
 *   <li>{@code service/BusinessNumberGenerator.java}：表示BusinessNumberGenerator领域对象或组件，封装该概念相关的数据和行为。</li>
 *   <li>{@code service/EntitlementExamPublishQuotaService.java}：实现权益考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/EntitlementService.java}：实现权益业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/MockPaymentService.java}：实现模拟支付业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/OrderService.java}：实现订单业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code service/ProductService.java}：实现商品业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。</li>
 *   <li>{@code web/AdminOrderController.java}：提供管理订单相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/EntitlementController.java}：提供权益相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/OrderController.java}：提供订单相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 *   <li>{@code web/ProductController.java}：提供商品相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。</li>
 * </ul>
 * <!-- FILE_INDEX_END -->
*/
package com.learningplatform.order;
