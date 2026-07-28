/* 文件职责：表示BaseEntity领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：公共数据模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.model;

import java.time.LocalDateTime;

/**
 * 表示BaseEntity领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：遵守 统一协议、异常、配置与跨领域基础设施 模块的职责边界。</p>
 */
public abstract class BaseEntity {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;
    /** 保存更新时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime updatedAt;
    /** 保存创建按，供该类型的业务逻辑读取或更新。 */
    private Long createdBy;
    /** 保存更新按，供该类型的业务逻辑读取或更新。 */
    private Long updatedBy;
    /** 保存deleted，供该类型的业务逻辑读取或更新。 */
    private Boolean deleted;

    /** 返回ID。 */
    public Long getId() {
        return id;
    }

    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** 返回更新时间。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 更新更新时间；调用方仍需遵守所属领域的校验规则。 */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** 返回创建按。 */
    public Long getCreatedBy() {
        return createdBy;
    }

    /** 更新创建按；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    /** 返回更新按。 */
    public Long getUpdatedBy() {
        return updatedBy;
    }

    /** 更新更新按；调用方仍需遵守所属领域的校验规则。 */
    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    /** 返回Deleted。 */
    public Boolean getDeleted() {
        return deleted;
    }

    /** 更新Deleted；调用方仍需遵守所属领域的校验规则。 */
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}

