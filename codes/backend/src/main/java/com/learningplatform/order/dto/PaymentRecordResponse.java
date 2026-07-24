package com.learningplatform.order.dto;

import com.learningplatform.order.domain.PaymentProvider;
import com.learningplatform.order.domain.PaymentRecord;
import com.learningplatform.order.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
