package com.learningplatform.classroom.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

public class ClassMember extends BaseEntity {
    private Long classId;
    private Long userId;
    private ClassRole role;
    private ClassMemberStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    public Long getClassId() { return classId; }
    public void setClassId(Long classId) { this.classId = classId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public ClassRole getRole() { return role; }
    public void setRole(ClassRole role) { this.role = role; }
    public ClassMemberStatus getStatus() { return status; }
    public void setStatus(ClassMemberStatus status) { this.status = status; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
    public LocalDateTime getLeftAt() { return leftAt; }
    public void setLeftAt(LocalDateTime leftAt) { this.leftAt = leftAt; }
}
