package com.learningplatform.order.dto;

import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;

import java.time.LocalDateTime;

public record EntitlementResponse(
        Long id,
        Long userId,
        EntitlementType entitlementType,
        Long resourceId,
        Long sourceOrderItemId,
        Integer totalQuantity,
        Integer availableQuantity,
        EntitlementStatus status,
        LocalDateTime effectiveAt,
        LocalDateTime expiresAt,
        int version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static EntitlementResponse from(UserEntitlement entitlement) {
        return new EntitlementResponse(
                entitlement.getId(),
                entitlement.getUserId(),
                entitlement.getEntitlementType(),
                entitlement.getResourceId(),
                entitlement.getSourceOrderItemId(),
                entitlement.getTotalQuantity(),
                entitlement.getAvailableQuantity(),
                entitlement.getStatus(),
                entitlement.getEffectiveAt(),
                entitlement.getExpiresAt(),
                entitlement.getVersion(),
                entitlement.getCreatedAt(),
                entitlement.getUpdatedAt()
        );
    }
}
