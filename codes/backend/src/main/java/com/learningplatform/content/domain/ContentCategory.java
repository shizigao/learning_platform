/* 文件职责：表示学习资料分类领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示学习资料分类领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ContentCategory extends BaseEntity {
    /** 保存parentID，供该类型的业务逻辑读取或更新。 */
    private Long parentId;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存slug，供该类型的业务逻辑读取或更新。 */
    private String slug;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;
    /** 保存启用状态，供该类型的业务逻辑读取或更新。 */
    private Boolean enabled;

    /** 返回ParentID。 */
    public Long getParentId() {
        return parentId;
    }

    /** 更新ParentID；调用方仍需遵守所属领域的校验规则。 */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /** 返回名称。 */
    public String getName() {
        return name;
    }

    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回Slug。 */
    public String getSlug() {
        return slug;
    }

    /** 更新Slug；调用方仍需遵守所属领域的校验规则。 */
    public void setSlug(String slug) {
        this.slug = slug;
    }

    /** 返回Description。 */
    public String getDescription() {
        return description;
    }

    /** 更新Description；调用方仍需遵守所属领域的校验规则。 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 返回启用状态。 */
    public Boolean getEnabled() {
        return enabled;
    }

    /** 更新启用状态；调用方仍需遵守所属领域的校验规则。 */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
