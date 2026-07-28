/* 文件职责：表示角色领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.domain;

import java.time.LocalDateTime;

/**
 * 表示角色领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class Role {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存编码，供该类型的业务逻辑读取或更新。 */
    private RoleCode code;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存启用状态，供该类型的业务逻辑读取或更新。 */
    private Boolean enabled;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;
    /** 保存更新时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime updatedAt;

    /** 返回ID。 */
    public Long getId() {
        return id;
    }

    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回编码。 */
    public RoleCode getCode() {
        return code;
    }

    /** 更新编码；调用方仍需遵守所属领域的校验规则。 */
    public void setCode(RoleCode code) {
        this.code = code;
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

    /** 返回启用状态。 */
    public Boolean getEnabled() {
        return enabled;
    }

    /** 更新启用状态；调用方仍需遵守所属领域的校验规则。 */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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
}
