/* 文件职责：定义权益响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import com.learningplatform.order.domain.EntitlementStatus;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;

import java.time.LocalDateTime;

/**
 * 定义权益响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
    /** 转换或规范化数据，不引入额外持久化副作用。 */
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
