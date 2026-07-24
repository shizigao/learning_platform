package com.learningplatform.order.dto;

import com.learningplatform.order.domain.Product;
import com.learningplatform.order.domain.ProductStatus;
import com.learningplatform.order.domain.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String productCode,
        ProductType productType,
        String name,
        String description,
        Long resourceId,
        Integer quantity,
        BigDecimal price,
        ProductStatus status,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductType(),
                product.getName(),
                product.getDescription(),
                product.getResourceId(),
                product.getQuantity(),
                product.getPrice(),
                product.getStatus(),
                product.getSortOrder(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
