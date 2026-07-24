package com.learningplatform.order.dto;

import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNo,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        BigDecimal payableAmount,
        BigDecimal paidAmount,
        String paymentMethod,
        String remark,
        LocalDateTime expiresAt,
        LocalDateTime paidAt,
        LocalDateTime cancelledAt,
        List<OrderItemResponse> items,
        List<PaymentRecordResponse> payments,
        String paymentNotice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OrderResponse {
        items = List.copyOf(items);
        payments = List.copyOf(payments);
    }

    public static OrderResponse from(
            Order order,
            List<OrderItemResponse> items,
            List<PaymentRecordResponse> payments
    ) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getPayableAmount(),
                order.getPaidAmount(),
                order.getPaymentMethod(),
                order.getRemark(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                items,
                payments,
                "当前仅为模拟支付，不会产生真实资金交易",
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
