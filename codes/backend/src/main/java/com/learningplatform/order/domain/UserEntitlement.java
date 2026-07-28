/* 文件职责：表示用户权益领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * 表示用户权益领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class UserEntitlement extends BaseEntity {
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存权益类型，供该类型的业务逻辑读取或更新。 */
    private EntitlementType entitlementType;
    /** 保存resourceID，供该类型的业务逻辑读取或更新。 */
    private Long resourceId;
    /** 保存来源订单ItemID，供该类型的业务逻辑读取或更新。 */
    private Long sourceOrderItemId;
    /** 保存总计Quantity，供该类型的业务逻辑读取或更新。 */
    private Integer totalQuantity;
    /** 保存可用Quantity，供该类型的业务逻辑读取或更新。 */
    private Integer availableQuantity;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private EntitlementStatus status;
    /** 保存生效时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime effectiveAt;
    /** 保存过期时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime expiresAt;
    /** 保存version，供该类型的业务逻辑读取或更新。 */
    private Integer version;

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回权益类型。 */
    public EntitlementType getEntitlementType() {
        return entitlementType;
    }

    /** 更新权益类型；调用方仍需遵守所属领域的校验规则。 */
    public void setEntitlementType(EntitlementType entitlementType) {
        this.entitlementType = entitlementType;
    }

    /** 返回ResourceID。 */
    public Long getResourceId() {
        return resourceId;
    }

    /** 更新ResourceID；调用方仍需遵守所属领域的校验规则。 */
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    /** 返回来源订单ItemID。 */
    public Long getSourceOrderItemId() {
        return sourceOrderItemId;
    }

    /** 更新来源订单ItemID；调用方仍需遵守所属领域的校验规则。 */
    public void setSourceOrderItemId(Long sourceOrderItemId) {
        this.sourceOrderItemId = sourceOrderItemId;
    }

    /** 返回总计Quantity。 */
    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    /** 更新总计Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    /** 返回可用Quantity。 */
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    /** 更新可用Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    /** 返回状态。 */
    public EntitlementStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(EntitlementStatus status) {
        this.status = status;
    }

    /** 返回生效时间。 */
    public LocalDateTime getEffectiveAt() {
        return effectiveAt;
    }

    /** 更新生效时间；调用方仍需遵守所属领域的校验规则。 */
    public void setEffectiveAt(LocalDateTime effectiveAt) {
        this.effectiveAt = effectiveAt;
    }

    /** 返回过期时间。 */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /** 更新过期时间；调用方仍需遵守所属领域的校验规则。 */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /** 返回Version。 */
    public Integer getVersion() {
        return version;
    }

    /** 更新Version；调用方仍需遵守所属领域的校验规则。 */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
