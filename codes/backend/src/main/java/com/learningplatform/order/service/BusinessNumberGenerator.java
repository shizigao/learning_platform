package com.learningplatform.order.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BusinessNumberGenerator {
    public String nextOrderNo() {
        return next("ORD");
    }

    public String nextPaymentNo() {
        return next("PAY");
    }

    public String nextMockTransactionNo() {
        return next("MOCK");
    }

    private String next(String prefix) {
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + random.substring(0, 24);
    }
}
