package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfflineTeacherApplication {
    private Long id;
    private Long userId;
    private String teacherName;
    private byte[] idCardCiphertext;
    private byte[] idCardIv;
    private byte[] idCardHmac;
    private String idCardMasked;
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
    private TeacherApplicationStatus status;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public byte[] getIdCardCiphertext() { return idCardCiphertext; }
    public void setIdCardCiphertext(byte[] value) { this.idCardCiphertext = value; }
    public byte[] getIdCardIv() { return idCardIv; }
    public void setIdCardIv(byte[] value) { this.idCardIv = value; }
    public byte[] getIdCardHmac() { return idCardHmac; }
    public void setIdCardHmac(byte[] value) { this.idCardHmac = value; }
    public String getIdCardMasked() { return idCardMasked; }
    public void setIdCardMasked(String value) { this.idCardMasked = value; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String value) { this.educationLevel = value; }
    public String getEducationBackground() { return educationBackground; }
    public void setEducationBackground(String value) { this.educationBackground = value; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
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
    public TeacherApplicationStatus getStatus() { return status; }
    public void setStatus(TeacherApplicationStatus status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String value) { this.rejectionReason = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { this.submittedAt = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { this.reviewedAt = value; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long value) { this.reviewedBy = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { this.version = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
