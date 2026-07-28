/* 文件职责：表示班级成员领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * 表示班级成员领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ClassMember extends BaseEntity {
    /** 保存班级ID，供该类型的业务逻辑读取或更新。 */
    private Long classId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存角色，供该类型的业务逻辑读取或更新。 */
    private ClassRole role;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ClassMemberStatus status;
    /** 保存joined时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime joinedAt;
    /** 保存left时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime leftAt;

    /** 返回班级ID。 */
    public Long getClassId() { return classId; }
    /** 更新班级ID；调用方仍需遵守所属领域的校验规则。 */
    public void setClassId(Long classId) { this.classId = classId; }
    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) { this.userId = userId; }
    /** 返回角色。 */
    public ClassRole getRole() { return role; }
    /** 更新角色；调用方仍需遵守所属领域的校验规则。 */
    public void setRole(ClassRole role) { this.role = role; }
    /** 返回状态。 */
    public ClassMemberStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ClassMemberStatus status) { this.status = status; }
    /** 返回Joined时间。 */
    public LocalDateTime getJoinedAt() { return joinedAt; }
    /** 更新Joined时间；调用方仍需遵守所属领域的校验规则。 */
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    /** 返回Left时间。 */
    public LocalDateTime getLeftAt() { return leftAt; }
    /** 更新Left时间；调用方仍需遵守所属领域的校验规则。 */
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
}
