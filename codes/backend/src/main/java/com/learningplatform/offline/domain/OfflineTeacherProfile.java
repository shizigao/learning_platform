package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfflineTeacherProfile {
    private Long id;
    private Long userId;
    private Long sourceApplicationId;
    private String teacherName;
    private String gender;
    private String educationLevel;
    private String educationBackground;
    private String institution;
    private String province;
    private String city;
    private String district;
    private String bio;
    private String teachingContent;
    private String teachingTags;
    private String availability;
    private BigDecimal hourlyRate;
    private String priceDescription;
    private String contactWechat;
    private String contactQq;
    private String contactEmail;
    private TeacherProfileStatus status;
    private String suspendedReason;
    private LocalDateTime approvedAt;
    private Long approvedBy;
    private String username;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long value) { this.id = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public Long getSourceApplicationId() { return sourceApplicationId; }
    public void setSourceApplicationId(Long value) { this.sourceApplicationId = value; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String value) { this.teacherName = value; }
    public String getGender() { return gender; }
    public void setGender(String value) { this.gender = value; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String value) { this.educationLevel = value; }
    public String getEducationBackground() { return educationBackground; }
    public void setEducationBackground(String value) { this.educationBackground = value; }
    public String getInstitution() { return institution; }
    public void setInstitution(String value) { this.institution = value; }
    public String getProvince() { return province; }
    public void setProvince(String value) { this.province = value; }
    public String getCity() { return city; }
    public void setCity(String value) { this.city = value; }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { this.district = value; }
    public String getBio() { return bio; }
    public void setBio(String value) { this.bio = value; }
    public String getTeachingContent() { return teachingContent; }
    public void setTeachingContent(String value) { this.teachingContent = value; }
    public String getTeachingTags() { return teachingTags; }
    public void setTeachingTags(String value) { this.teachingTags = value; }
    public String getAvailability() { return availability; }
    public void setAvailability(String value) { this.availability = value; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal value) { this.hourlyRate = value; }
    public String getPriceDescription() { return priceDescription; }
    public void setPriceDescription(String value) { this.priceDescription = value; }
    public String getContactWechat() { return contactWechat; }
    public void setContactWechat(String value) { this.contactWechat = value; }
    public String getContactQq() { return contactQq; }
    public void setContactQq(String value) { this.contactQq = value; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String value) { this.contactEmail = value; }
    public TeacherProfileStatus getStatus() { return status; }
    public void setStatus(TeacherProfileStatus value) { this.status = value; }
    public String getSuspendedReason() { return suspendedReason; }
    public void setSuspendedReason(String value) { this.suspendedReason = value; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime value) { this.approvedAt = value; }
    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long value) { this.approvedBy = value; }
    public String getUsername() { return username; }
    public void setUsername(String value) { this.username = value; }
    public String getNickname() { return nickname; }
    public void setNickname(String value) { this.nickname = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
