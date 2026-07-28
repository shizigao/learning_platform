/* 文件职责：定义订单响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义订单响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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

    /** 转换或规范化数据，不引入额外持久化副作用。 */
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
