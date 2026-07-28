/* 文件职责：实现订单业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderItem;
import com.learningplatform.order.domain.OrderStatus;
import com.learningplatform.order.domain.Product;
import com.learningplatform.order.domain.ProductType;
import com.learningplatform.order.dto.AdminOrderListQuery;
import com.learningplatform.order.dto.OrderCreateItemRequest;
import com.learningplatform.order.dto.OrderCreateRequest;
import com.learningplatform.order.dto.OrderItemResponse;
import com.learningplatform.order.dto.OrderListQuery;
import com.learningplatform.order.dto.OrderResponse;
import com.learningplatform.order.dto.PaymentRecordResponse;
import com.learningplatform.order.mapper.OrderItemMapper;
import com.learningplatform.order.mapper.OrderMapper;
import com.learningplatform.order.mapper.PaymentRecordMapper;
import com.learningplatform.user.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
/**
 * 实现订单业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class OrderService {
    /** 定义 PAYMENT_WINDOW_MINUTES 常量，统一该组件使用的固定规则或默认值。 */
    private static final int PAYMENT_WINDOW_MINUTES = 30;
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("99999999.99");

    /** 访问订单持久化数据。 */
    private final OrderMapper orderMapper;
    /** 访问item持久化数据。 */
    private final OrderItemMapper itemMapper;
    /** 访问支付持久化数据。 */
    private final PaymentRecordMapper paymentMapper;
    /** 委托商品执行对应领域规则。 */
    private final ProductService productService;
    /** 保存numberGenerator，供该类型的业务逻辑读取或更新。 */
    private final BusinessNumberGenerator numberGenerator;
    /** 委托权益执行对应领域规则。 */
    private final EntitlementService entitlementService;
    /** 访问用户持久化数据。 */
    private final UserMapper userMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OrderService(
            OrderMapper orderMapper,
            OrderItemMapper itemMapper,
            PaymentRecordMapper paymentMapper,
            ProductService productService,
            BusinessNumberGenerator numberGenerator,
            EntitlementService entitlementService,
            UserMapper userMapper
    ) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.paymentMapper = paymentMapper;
        this.productService = productService;
        this.numberGenerator = numberGenerator;
        this.entitlementService = entitlementService;
        this.userMapper = userMapper;
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public OrderResponse create(Long userId, OrderCreateRequest request) {
        LocalDateTime now = now();
        List<OrderItem> items = buildItems(request.items());
        lockAndValidateContentPurchase(userId, items, null);
        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(MAX_ORDER_AMOUNT) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单金额超过允许上限");
        }

        Order order = new Order();
        order.setOrderNo(numberGenerator.nextOrderNo());
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalAmount(total);
        order.setPayableAmount(total);
        order.setRemark(normalize(request.remark()));
        order.setExpiresAt(now.plusMinutes(PAYMENT_WINDOW_MINUTES));
        order.setVersion(0);
        if (orderMapper.insert(order) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建订单失败");
        }
        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            if (itemMapper.insert(item) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建订单明细失败");
            }
        }
        return response(order, items);
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<OrderResponse> list(Long userId, OrderListQuery query) {
        List<OrderResponse> items = orderMapper.findByUser(
                        userId,
                        query.getStatus(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::response)
                .toList();
        long total = orderMapper.countByUser(userId, query.getStatus());
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public OrderResponse detail(Long orderId, Long userId) {
        return response(requireOwned(orderId, userId));
    }

    /** 查询For管理相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<OrderResponse> listForAdmin(AdminOrderListQuery query) {
        List<OrderResponse> items = orderMapper.findAll(
                        query.getOrderNo(),
                        query.getUserId(),
                        query.getStatus(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::response)
                .toList();
        long total = orderMapper.countAll(
                query.getOrderNo(),
                query.getUserId(),
                query.getStatus()
        );
        return PageResult.of(
                items,
                total,
                query.getPageNumber(),
                query.getPageSize()
        );
    }

    /** 查询For管理相关数据；只返回当前调用方有权查看的结果。 */
    public OrderResponse detailForAdmin(Long orderId) {
        return response(orderMapper.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "订单不存在"
                )));
    }

    @Transactional
    /** 判断是否满足cel条件，不修改持久化状态。 */
    public OrderResponse cancel(Long orderId, Long userId) {
        Order order = requireOwnedForUpdate(orderId, userId);
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "只有待支付订单可以取消");
        }
        if (orderMapper.cancel(orderId, userId, order.getVersion(), now()) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }
        return detail(orderId, userId);
    }

    Order requireOwnedForUpdate(Long orderId, Long userId) {
        Order order = orderMapper.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        assertOwner(order, userId);
        return order;
    }

    void assertContentPaymentAllowed(Long orderId, Long userId) {
        lockAndValidateContentPurchase(
                userId,
                itemMapper.findByOrderId(orderId),
                orderId
        );
    }

    OrderResponse response(Order order) {
        return response(order, itemMapper.findByOrderId(order.getId()));
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private OrderResponse response(Order order, List<OrderItem> items) {
        return OrderResponse.from(
                order,
                items.stream().map(OrderItemResponse::from).toList(),
                paymentMapper.findByOrderId(order.getId()).stream()
                        .map(PaymentRecordResponse::from)
                        .toList()
        );
    }

    /** 校验Owned及相关业务前置条件，不满足时抛出明确业务异常。 */
    private Order requireOwned(Long orderId, Long userId) {
        Order order = orderMapper.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        assertOwner(order, userId);
        return order;
    }

    /** 校验Owner及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void assertOwner(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
    }

    /** 执行 buildItems 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private List<OrderItem> buildItems(List<OrderCreateItemRequest> requests) {
        Set<Long> productIds = new HashSet<>();
        Set<Long> contentResourceIds = new HashSet<>();
        List<OrderItem> items = new ArrayList<>(requests.size());
        for (OrderCreateItemRequest request : requests) {
            if (!productIds.add(request.productId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "订单商品不能重复");
            }
            Product product = productService.getPurchasable(request.productId());
            if (product.getProductType() == ProductType.CONTENT && request.quantity() != 1) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "付费资料商品的购买数量只能为1"
                );
            }
            if (product.getProductType() == ProductType.CONTENT
                    && !contentResourceIds.add(product.getResourceId())) {
                throw new BusinessException(
                        ErrorCode.BAD_REQUEST,
                        "同一付费资料不能在订单中重复购买"
                );
            }
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(request.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);
            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductCodeSnapshot(product.getProductCode());
            item.setProductTypeSnapshot(product.getProductType());
            item.setProductNameSnapshot(product.getName());
            item.setResourceIdSnapshot(product.getResourceId());
            item.setUnitPrice(product.getPrice().setScale(2, RoundingMode.HALF_UP));
            item.setQuantity(request.quantity());
            item.setEntitlementQuantity(product.getQuantity());
            item.setSubtotalAmount(subtotal);
            items.add(item);
        }
        return List.copyOf(items);
    }

    /** 执行 lockAndValidateContentPurchase 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void lockAndValidateContentPurchase(
            Long userId,
            List<OrderItem> items,
            Long excludedOrderId
    ) {
        List<Long> resourceIds = items.stream()
                .filter(item -> item.getProductTypeSnapshot() == ProductType.CONTENT)
                .map(OrderItem::getResourceIdSnapshot)
                .distinct()
                .toList();
        if (resourceIds.isEmpty()) {
            return;
        }
        userMapper.lockById(userId).orElseThrow(() ->
                new BusinessException(ErrorCode.NOT_FOUND, "用户不存在")
        );
        for (Long resourceId : resourceIds) {
            if (entitlementService.hasActiveContentAccess(userId, resourceId)) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "你已拥有该付费资料，无需重复购买"
                );
            }
            if (orderMapper.existsBlockingContentOrder(
                    userId,
                    resourceId,
                    excludedOrderId
            )) {
                throw new BusinessException(
                        ErrorCode.CONFLICT,
                        "该付费资料已购买或存在待支付订单，请勿重复购买"
                );
            }
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 now 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
