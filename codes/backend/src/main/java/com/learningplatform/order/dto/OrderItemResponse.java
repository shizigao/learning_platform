/* 文件职责：定义订单Item响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import com.learningplatform.order.domain.OrderItem;
import com.learningplatform.order.domain.ProductType;

import java.math.BigDecimal;

/**
 * 定义订单Item响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
    /** 转换或规范化数据，不引入额外持久化副作用。 */
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
