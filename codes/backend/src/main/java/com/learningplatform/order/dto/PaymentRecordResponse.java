/* 文件职责：定义支付记录响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import com.learningplatform.order.domain.PaymentProvider;
import com.learningplatform.order.domain.PaymentRecord;
import com.learningplatform.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定义支付记录响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record PaymentRecordResponse(
        Long id,
        String paymentNo,
        PaymentProvider provider,
        String providerTransactionNo,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        LocalDateTime paidAt,
        LocalDateTime createdAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static PaymentRecordResponse from(PaymentRecord payment) {
        return new PaymentRecordResponse(
                payment.getId(),
                payment.getPaymentNo(),
                payment.getProvider(),
                payment.getProviderTransactionNo(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getPaidAt(),
                payment.getCreatedAt()
        );
    }
}
