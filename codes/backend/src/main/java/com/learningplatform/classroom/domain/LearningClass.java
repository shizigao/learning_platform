/* 文件职责：表示学习班级领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示学习班级领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class LearningClass extends BaseEntity {
    /** 保存ownerID，供该类型的业务逻辑读取或更新。 */
    private Long ownerId;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存邀请码编码，供该类型的业务逻辑读取或更新。 */
    private String inviteCode;
    /** 保存邀请码启用状态，供该类型的业务逻辑读取或更新。 */
    private Boolean inviteEnabled;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ClassStatus status;
    /** 保存version，供该类型的业务逻辑读取或更新。 */
    private Integer version;

    /** 返回OwnerID。 */
    public Long getOwnerId() { return ownerId; }
    /** 更新OwnerID；调用方仍需遵守所属领域的校验规则。 */
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    /** 返回名称。 */
    public String getName() { return name; }
    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) { this.name = name; }
    /** 返回Description。 */
    public String getDescription() { return description; }
    /** 更新Description；调用方仍需遵守所属领域的校验规则。 */
    public void setDescription(String description) { this.description = description; }
    /** 返回邀请码编码。 */
    public String getInviteCode() { return inviteCode; }
    /** 更新邀请码编码；调用方仍需遵守所属领域的校验规则。 */
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    /** 返回邀请码启用状态。 */
    public Boolean getInviteEnabled() { return inviteEnabled; }
    /** 更新邀请码启用状态；调用方仍需遵守所属领域的校验规则。 */
    public void setInviteEnabled(Boolean inviteEnabled) { this.inviteEnabled = inviteEnabled; }
    /** 返回状态。 */
    public ClassStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ClassStatus status) { this.status = status; }
    /** 返回Version。 */
    public Integer getVersion() { return version; }
    /** 更新Version；调用方仍需遵守所属领域的校验规则。 */
    public void setVersion(Integer version) { this.version = version; }
}
