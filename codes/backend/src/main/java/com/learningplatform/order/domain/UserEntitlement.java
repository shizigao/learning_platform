package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

public class UserEntitlement extends BaseEntity {
    private Long userId;
    private EntitlementType entitlementType;
    private Long resourceId;
    private Long sourceOrderItemId;
    private Integer totalQuantity;
    private Integer availableQuantity;
    private EntitlementStatus status;
    private LocalDateTime effectiveAt;
    private LocalDateTime expiresAt;
    private Integer version;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public EntitlementType getEntitlementType() {
        return entitlementType;
    }

    public void setEntitlementType(EntitlementType entitlementType) {
        this.entitlementType = entitlementType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Long getSourceOrderItemId() {
        return sourceOrderItemId;
    }

    public void setSourceOrderItemId(Long sourceOrderItemId) {
        this.sourceOrderItemId = sourceOrderItemId;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public EntitlementStatus getStatus() {
        return status;
    }

    public void setStatus(EntitlementStatus status) {
        this.status = status;
    }

    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public void setEffectiveAt(LocalDateTime effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
