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
public class OrderService {
    private static final int PAYMENT_WINDOW_MINUTES = 30;
    private static final BigDecimal MAX_ORDER_AMOUNT = new BigDecimal("99999999.99");

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final PaymentRecordMapper paymentMapper;
    private final ProductService productService;
    private final BusinessNumberGenerator numberGenerator;

    public OrderService(
            OrderMapper orderMapper,
            OrderItemMapper itemMapper,
            PaymentRecordMapper paymentMapper,
            ProductService productService,
            BusinessNumberGenerator numberGenerator
    ) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.paymentMapper = paymentMapper;
        this.productService = productService;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public OrderResponse create(Long userId, OrderCreateRequest request) {
        LocalDateTime now = now();
        List<OrderItem> items = buildItems(request.items());
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

    public OrderResponse detail(Long orderId, Long userId) {
        return response(requireOwned(orderId, userId));
    }

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

    public OrderResponse detailForAdmin(Long orderId) {
        return response(orderMapper.findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "订单不存在"
                )));
    }

    @Transactional
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

    OrderResponse response(Order order) {
        return response(order, itemMapper.findByOrderId(order.getId()));
    }

    private OrderResponse response(Order order, List<OrderItem> items) {
        return OrderResponse.from(
                order,
                items.stream().map(OrderItemResponse::from).toList(),
                paymentMapper.findByOrderId(order.getId()).stream()
                        .map(PaymentRecordResponse::from)
                        .toList()
        );
    }

    private Order requireOwned(Long orderId, Long userId) {
        Order order = orderMapper.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "订单不存在"));
        assertOwner(order, userId);
        return order;
    }

    private void assertOwner(Order order, Long userId) {
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
        }
    }

    private List<OrderItem> buildItems(List<OrderCreateItemRequest> requests) {
        Set<Long> productIds = new HashSet<>();
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
