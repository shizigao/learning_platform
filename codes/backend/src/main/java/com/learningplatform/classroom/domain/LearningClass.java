package com.learningplatform.classroom.domain;

import com.learningplatform.common.model.BaseEntity;

public class LearningClass extends BaseEntity {
    private Long ownerId;
    private String name;
    private String description;
    private String inviteCode;
    private Boolean inviteEnabled;
    private ClassStatus status;
    private Integer version;

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public Boolean getInviteEnabled() { return inviteEnabled; }
    public void setInviteEnabled(Boolean inviteEnabled) { this.inviteEnabled = inviteEnabled; }
    public ClassStatus getStatus() { return status; }
    public void setStatus(ClassStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
