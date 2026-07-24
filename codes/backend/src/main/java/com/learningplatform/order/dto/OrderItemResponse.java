package com.learningplatform.order.dto;

import com.learningplatform.order.domain.OrderItem;
import com.learningplatform.order.domain.ProductType;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productCode,
        ProductType productType,
        String productName,
        Long resourceId,
        BigDecimal unitPrice,
        int quantity,
        Integer entitlementQuantity,
        BigDecimal subtotalAmount
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductCodeSnapshot(),
                item.getProductTypeSnapshot(),
                item.getProductNameSnapshot(),
                item.getResourceIdSnapshot(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getEntitlementQuantity(),
                item.getSubtotalAmount()
        );
    }
}
