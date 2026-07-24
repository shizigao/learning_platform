package com.learningplatform.order.dto;

public record MockPaymentResponse(
        OrderResponse order,
        PaymentRecordResponse payment,
        String notice
) {
}
