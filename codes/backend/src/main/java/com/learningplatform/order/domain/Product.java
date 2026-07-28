/* 文件职责：表示商品领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

/**
 * 表示商品领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class Product extends BaseEntity {
    /** 保存商品编码，供该类型的业务逻辑读取或更新。 */
    private String productCode;
    /** 保存商品类型，供该类型的业务逻辑读取或更新。 */
    private ProductType productType;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存resourceID，供该类型的业务逻辑读取或更新。 */
    private Long resourceId;
    /** 保存quantity，供该类型的业务逻辑读取或更新。 */
    private Integer quantity;
    /** 保存价格，供该类型的业务逻辑读取或更新。 */
    private BigDecimal price;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ProductStatus status;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;

    /** 返回商品编码。 */
    public String getProductCode() {
        return productCode;
    }

    /** 更新商品编码；调用方仍需遵守所属领域的校验规则。 */
    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    /** 返回商品类型。 */
    public ProductType getProductType() {
        return productType;
    }

    /** 更新商品类型；调用方仍需遵守所属领域的校验规则。 */
    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    /** 返回名称。 */
    public String getName() {
        return name;
    }

    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回Description。 */
    public String getDescription() {
        return description;
    }

    /** 更新Description；调用方仍需遵守所属领域的校验规则。 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 返回ResourceID。 */
    public Long getResourceId() {
        return resourceId;
    }

    /** 更新ResourceID；调用方仍需遵守所属领域的校验规则。 */
    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    /** 返回Quantity。 */
    public Integer getQuantity() {
        return quantity;
    }

    /** 更新Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    /** 返回价格。 */
    public BigDecimal getPrice() {
        return price;
    }

    /** 更新价格；调用方仍需遵守所属领域的校验规则。 */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /** 返回状态。 */
    public ProductStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
