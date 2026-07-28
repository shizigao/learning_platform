/* 文件职责：表示用户角色领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.domain;

import java.time.LocalDateTime;

/**
 * 表示用户角色领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class UserRole {
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存角色ID，供该类型的业务逻辑读取或更新。 */
    private Long roleId;
    /** 保存granted按，供该类型的业务逻辑读取或更新。 */
    private Long grantedBy;
    /** 保存granted时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime grantedAt;

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回角色ID。 */
    public Long getRoleId() {
        return roleId;
    }

    /** 更新角色ID；调用方仍需遵守所属领域的校验规则。 */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /** 返回Granted按。 */
    public Long getGrantedBy() {
        return grantedBy;
    }

    /** 更新Granted按；调用方仍需遵守所属领域的校验规则。 */
    public void setGrantedBy(Long grantedBy) {
        this.grantedBy = grantedBy;
    }

    /** 返回Granted时间。 */
    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    /** 更新Granted时间；调用方仍需遵守所属领域的校验规则。 */
    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }
}
