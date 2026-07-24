package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.Order;
import com.learningplatform.order.domain.OrderStatus;
import com.learningplatform.order.domain.PaymentProvider;
import com.learningplatform.order.domain.PaymentRecord;
import com.learningplatform.order.domain.PaymentStatus;
import com.learningplatform.order.dto.MockPaymentResponse;
import com.learningplatform.order.dto.OrderResponse;
import com.learningplatform.order.dto.PaymentRecordResponse;
import com.learningplatform.order.mapper.OrderMapper;
import com.learningplatform.order.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class MockPaymentService {
    private final OrderMapper orderMapper;
    private final PaymentRecordMapper paymentMapper;
    private final OrderService orderService;
    private final EntitlementService entitlementService;
    private final BusinessNumberGenerator numberGenerator;

    public MockPaymentService(
            OrderMapper orderMapper,
            PaymentRecordMapper paymentMapper,
            OrderService orderService,
            EntitlementService entitlementService,
            BusinessNumberGenerator numberGenerator
    ) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
        this.entitlementService = entitlementService;
        this.numberGenerator = numberGenerator;
    }

    @Transactional
    public MockPaymentResponse pay(Long orderId, Long userId) {
        Order order = orderService.requireOwnedForUpdate(orderId, userId);
        if (order.getStatus() == OrderStatus.PAID) {
            PaymentRecord existingPayment = paymentMapper.findSuccessfulByOrderId(orderId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "已支付订单缺少成功支付记录"
                    ));
            entitlementService.grantForPaidOrder(orderId, userId);
            return response(orderId, userId, existingPayment);
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单不是待支付状态");
        }
        LocalDateTime paidAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        if (order.getExpiresAt() != null && !paidAt.isBefore(order.getExpiresAt())) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单已超过支付期限");
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setPaymentNo(numberGenerator.nextPaymentNo());
        payment.setOrderId(orderId);
        payment.setProvider(PaymentProvider.MOCK);
        payment.setProviderTransactionNo(numberGenerator.nextMockTransactionNo());
        payment.setAmount(order.getPayableAmount());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setRequestPayload("{\"mode\":\"MOCK\"}");
        payment.setResponsePayload("{\"result\":\"SIMULATED_SUCCESS\"}");
        payment.setPaidAt(paidAt);

        if (paymentMapper.insert(payment) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建模拟支付记录失败");
        }
        if (orderMapper.markMockPaid(
                orderId,
                userId,
                order.getVersion(),
                order.getPayableAmount(),
                paidAt
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "订单状态已变化，请刷新后重试");
        }
        entitlementService.grantForPaidOrder(orderId, userId);
        return response(orderId, userId, payment);
    }

    private MockPaymentResponse response(
            Long orderId,
            Long userId,
            PaymentRecord payment
    ) {
        OrderResponse paidOrder = orderService.detail(orderId, userId);
        return new MockPaymentResponse(
                paidOrder,
                PaymentRecordResponse.from(payment),
                "模拟支付成功：本次操作不会产生真实资金交易"
        );
    }
}
