package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

public class OrderItem extends BaseEntity {
    private Long orderId;
    private Long productId;
    private String productCodeSnapshot;
    private ProductType productTypeSnapshot;
    private String productNameSnapshot;
    private Long resourceIdSnapshot;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer entitlementQuantity;
    private BigDecimal subtotalAmount;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCodeSnapshot() {
        return productCodeSnapshot;
    }

    public void setProductCodeSnapshot(String productCodeSnapshot) {
        this.productCodeSnapshot = productCodeSnapshot;
    }

    public ProductType getProductTypeSnapshot() {
        return productTypeSnapshot;
    }

    public void setProductTypeSnapshot(ProductType productTypeSnapshot) {
        this.productTypeSnapshot = productTypeSnapshot;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public void setProductNameSnapshot(String productNameSnapshot) {
        this.productNameSnapshot = productNameSnapshot;
    }

    public Long getResourceIdSnapshot() {
        return resourceIdSnapshot;
    }

    public void setResourceIdSnapshot(Long resourceIdSnapshot) {
        this.resourceIdSnapshot = resourceIdSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getEntitlementQuantity() {
        return entitlementQuantity;
    }

    public void setEntitlementQuantity(Integer entitlementQuantity) {
        this.entitlementQuantity = entitlementQuantity;
    }

    public BigDecimal getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(BigDecimal subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
    }
}
