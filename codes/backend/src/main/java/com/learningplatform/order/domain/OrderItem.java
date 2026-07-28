/* 文件职责：表示订单Item领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

/**
 * 表示订单Item领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class OrderItem extends BaseEntity {
    /** 保存订单ID，供该类型的业务逻辑读取或更新。 */
    private Long orderId;
    /** 保存商品ID，供该类型的业务逻辑读取或更新。 */
    private Long productId;
    /** 保存商品编码Snapshot，供该类型的业务逻辑读取或更新。 */
    private String productCodeSnapshot;
    /** 保存商品类型Snapshot，供该类型的业务逻辑读取或更新。 */
    private ProductType productTypeSnapshot;
    /** 保存商品名称Snapshot，供该类型的业务逻辑读取或更新。 */
    private String productNameSnapshot;
    /** 保存resourceIDSnapshot，供该类型的业务逻辑读取或更新。 */
    private Long resourceIdSnapshot;
    /** 保存unit价格，供该类型的业务逻辑读取或更新。 */
    private BigDecimal unitPrice;
    /** 保存quantity，供该类型的业务逻辑读取或更新。 */
    private Integer quantity;
    /** 保存权益Quantity，供该类型的业务逻辑读取或更新。 */
    private Integer entitlementQuantity;
    /** 保存subtotalAmount，供该类型的业务逻辑读取或更新。 */
    private BigDecimal subtotalAmount;

    /** 返回订单ID。 */
    public Long getOrderId() {
        return orderId;
    }

    /** 更新订单ID；调用方仍需遵守所属领域的校验规则。 */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /** 返回商品ID。 */
    public Long getProductId() {
        return productId;
    }

    /** 更新商品ID；调用方仍需遵守所属领域的校验规则。 */
    public void setProductId(Long productId) {
        this.productId = productId;
    }

    /** 返回商品编码Snapshot。 */
    public String getProductCodeSnapshot() {
        return productCodeSnapshot;
    }

    /** 更新商品编码Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setProductCodeSnapshot(String productCodeSnapshot) {
        this.productCodeSnapshot = productCodeSnapshot;
    }

    /** 返回商品类型Snapshot。 */
    public ProductType getProductTypeSnapshot() {
        return productTypeSnapshot;
    }

    /** 更新商品类型Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setProductTypeSnapshot(ProductType productTypeSnapshot) {
        this.productTypeSnapshot = productTypeSnapshot;
    }

    /** 返回商品名称Snapshot。 */
    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    /** 更新商品名称Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    /** 返回ResourceIDSnapshot。 */
    public Long getResourceIdSnapshot() {
        return resourceIdSnapshot;
    }

    /** 更新ResourceIDSnapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setResourceIdSnapshot(Long resourceIdSnapshot) {
        this.resourceIdSnapshot = resourceIdSnapshot;
    }

    /** 返回Unit价格。 */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** 更新Unit价格；调用方仍需遵守所属领域的校验规则。 */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    /** 返回Quantity。 */
    public Integer getQuantity() {
        return quantity;
    }

    /** 更新Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /** 返回权益Quantity。 */
    public Integer getEntitlementQuantity() {
        return entitlementQuantity;
    }

    /** 更新权益Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setEntitlementQuantity(Integer entitlementQuantity) {
        this.entitlementQuantity = entitlementQuantity;
    }

    /** 返回SubtotalAmount。 */
    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    /** 更新SubtotalAmount；调用方仍需遵守所属领域的校验规则。 */
    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }
}
