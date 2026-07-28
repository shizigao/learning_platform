/* 文件职责：实现模拟支付业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现模拟支付业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class MockPaymentService {
    /** 访问订单持久化数据。 */
    private final OrderMapper orderMapper;
    /** 访问支付持久化数据。 */
    private final PaymentRecordMapper paymentMapper;
    /** 委托订单执行对应领域规则。 */
    private final OrderService orderService;
    /** 委托权益执行对应领域规则。 */
    private final EntitlementService entitlementService;
    /** 保存numberGenerator，供该类型的业务逻辑读取或更新。 */
    private final BusinessNumberGenerator numberGenerator;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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
    /** 执行 pay 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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
        orderService.assertContentPaymentAllowed(orderId, userId);

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

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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
