/* 文件职责：表示线下教学教师申请领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示线下教学教师申请领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class OfflineTeacherApplication {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存教师名称，供该类型的业务逻辑读取或更新。 */
    private String teacherName;
    /** 保存IDCardCiphertext，供该类型的业务逻辑读取或更新。 */
    private byte[] idCardCiphertext;
    /** 保存IDCardIv，供该类型的业务逻辑读取或更新。 */
    private byte[] idCardIv;
    /** 保存IDCardHmac，供该类型的业务逻辑读取或更新。 */
    private byte[] idCardHmac;
    /** 保存IDCardMasked，供该类型的业务逻辑读取或更新。 */
    private String idCardMasked;
    /** 保存gender，供该类型的业务逻辑读取或更新。 */
    private String gender;
    /** 保存educationLevel，供该类型的业务逻辑读取或更新。 */
    private String educationLevel;
    /** 保存educationBackground，供该类型的业务逻辑读取或更新。 */
    private String educationBackground;
    /** 保存institution，供该类型的业务逻辑读取或更新。 */
    private String institution;
    /** 保存province，供该类型的业务逻辑读取或更新。 */
    private String province;
    /** 保存city，供该类型的业务逻辑读取或更新。 */
    private String city;
    /** 保存district，供该类型的业务逻辑读取或更新。 */
    private String district;
    /** 保存bio，供该类型的业务逻辑读取或更新。 */
    private String bio;
    /** 保存教学学习资料，供该类型的业务逻辑读取或更新。 */
    private String teachingContent;
    /** 保存教学Tags，供该类型的业务逻辑读取或更新。 */
    private String teachingTags;
    /** 保存availability，供该类型的业务逻辑读取或更新。 */
    private String availability;
    /** 保存hourly频率，供该类型的业务逻辑读取或更新。 */
    private BigDecimal hourlyRate;
    /** 保存价格Description，供该类型的业务逻辑读取或更新。 */
    private String priceDescription;
    /** 保存contactWechat，供该类型的业务逻辑读取或更新。 */
    private String contactWechat;
    /** 保存contactQq，供该类型的业务逻辑读取或更新。 */
    private String contactQq;
    /** 保存contactEmail，供该类型的业务逻辑读取或更新。 */
    private String contactEmail;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private TeacherApplicationStatus status;
    /** 保存驳回原因，供该类型的业务逻辑读取或更新。 */
    private String rejectionReason;
    /** 保存提交时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime submittedAt;
    /** 保存reviewed时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime reviewedAt;
    /** 保存reviewed按，供该类型的业务逻辑读取或更新。 */
    private Long reviewedBy;
    /** 保存version，供该类型的业务逻辑读取或更新。 */
    private Integer version;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;
    /** 保存更新时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime updatedAt;

    /** 返回ID。 */
    public Long getId() { return id; }
    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long id) { this.id = id; }
    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) { this.userId = userId; }
    /** 返回教师名称。 */
    public String getTeacherName() { return teacherName; }
    /** 更新教师名称；调用方仍需遵守所属领域的校验规则。 */
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    /** 返回IDCardCiphertext。 */
    public byte[] getIdCardCiphertext() { return idCardCiphertext; }
    /** 更新IDCardCiphertext；调用方仍需遵守所属领域的校验规则。 */
    public void setIdCardCiphertext(byte[] value) { this.idCardCiphertext = value; }
    /** 返回IDCardIv。 */
    public byte[] getIdCardIv() { return idCardIv; }
    /** 更新IDCardIv；调用方仍需遵守所属领域的校验规则。 */
    public void setIdCardIv(byte[] value) { this.idCardIv = value; }
    /** 返回IDCardHmac。 */
    public byte[] getIdCardHmac() { return idCardHmac; }
    /** 更新IDCardHmac；调用方仍需遵守所属领域的校验规则。 */
    public void setIdCardHmac(byte[] value) { this.idCardHmac = value; }
    /** 返回IDCardMasked。 */
    public String getIdCardMasked() { return idCardMasked; }
    /** 更新IDCardMasked；调用方仍需遵守所属领域的校验规则。 */
    public void setIdCardMasked(String value) { this.idCardMasked = value; }
    /** 返回Gender。 */
    public String getGender() { return gender; }
    /** 更新Gender；调用方仍需遵守所属领域的校验规则。 */
    public void setGender(String gender) { this.gender = gender; }
    /** 返回EducationLevel。 */
    public String getEducationLevel() { return educationLevel; }
    /** 更新EducationLevel；调用方仍需遵守所属领域的校验规则。 */
    public void setEducationLevel(String value) { this.educationLevel = value; }
    /** 返回EducationBackground。 */
    public String getEducationBackground() { return educationBackground; }
    /** 更新EducationBackground；调用方仍需遵守所属领域的校验规则。 */
    public void setEducationBackground(String value) { this.educationBackground = value; }
    /** 返回Institution。 */
    public String getInstitution() { return institution; }
    /** 更新Institution；调用方仍需遵守所属领域的校验规则。 */
    public void setInstitution(String institution) { this.institution = institution; }
    /** 返回Province。 */
    public String getProvince() { return province; }
    /** 更新Province；调用方仍需遵守所属领域的校验规则。 */
    public void setProvince(String province) { this.province = province; }
    /** 返回City。 */
    public String getCity() { return city; }
    /** 更新City；调用方仍需遵守所属领域的校验规则。 */
    public void setCity(String city) { this.city = city; }
    /** 返回District。 */
    public String getDistrict() { return district; }
    /** 更新District；调用方仍需遵守所属领域的校验规则。 */
    public void setDistrict(String district) { this.district = district; }
    /** 返回Bio。 */
    public String getBio() { return bio; }
    /** 更新Bio；调用方仍需遵守所属领域的校验规则。 */
    public void setBio(String bio) { this.bio = bio; }
    /** 返回教学学习资料。 */
    public String getTeachingContent() { return teachingContent; }
    /** 更新教学学习资料；调用方仍需遵守所属领域的校验规则。 */
    public void setTeachingContent(String value) { this.teachingContent = value; }
    /** 返回教学Tags。 */
    public String getTeachingTags() { return teachingTags; }
    /** 更新教学Tags；调用方仍需遵守所属领域的校验规则。 */
    public void setTeachingTags(String value) { this.teachingTags = value; }
    /** 返回Availability。 */
    public String getAvailability() { return availability; }
    /** 更新Availability；调用方仍需遵守所属领域的校验规则。 */
    public void setAvailability(String value) { this.availability = value; }
    /** 返回Hourly频率。 */
    public BigDecimal getHourlyRate() { return hourlyRate; }
    /** 更新Hourly频率；调用方仍需遵守所属领域的校验规则。 */
    public void setHourlyRate(BigDecimal value) { this.hourlyRate = value; }
    /** 返回价格Description。 */
    public String getPriceDescription() { return priceDescription; }
    /** 更新价格Description；调用方仍需遵守所属领域的校验规则。 */
    public void setPriceDescription(String value) { this.priceDescription = value; }
    /** 返回ContactWechat。 */
    public String getContactWechat() { return contactWechat; }
    /** 更新ContactWechat；调用方仍需遵守所属领域的校验规则。 */
    public void setContactWechat(String value) { this.contactWechat = value; }
    /** 返回ContactQq。 */
    public String getContactQq() { return contactQq; }
    /** 更新ContactQq；调用方仍需遵守所属领域的校验规则。 */
    public void setContactQq(String value) { this.contactQq = value; }
    /** 返回ContactEmail。 */
    public String getContactEmail() { return contactEmail; }
    /** 更新ContactEmail；调用方仍需遵守所属领域的校验规则。 */
    public void setContactEmail(String value) { this.contactEmail = value; }
    /** 返回状态。 */
    public TeacherApplicationStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(TeacherApplicationStatus status) { this.status = status; }
    /** 返回驳回原因。 */
    public String getRejectionReason() { return rejectionReason; }
    /** 更新驳回原因；调用方仍需遵守所属领域的校验规则。 */
    public void setRejectionReason(String value) { this.rejectionReason = value; }
    /** 返回提交时间。 */
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    /** 更新提交时间；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmittedAt(LocalDateTime value) { this.submittedAt = value; }
    /** 返回Reviewed时间。 */
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    /** 更新Reviewed时间；调用方仍需遵守所属领域的校验规则。 */
    public void setReviewedAt(LocalDateTime value) { this.reviewedAt = value; }
    /** 返回Reviewed按。 */
    public Long getReviewedBy() { return reviewedBy; }
    /** 更新Reviewed按；调用方仍需遵守所属领域的校验规则。 */
    public void setReviewedBy(Long value) { this.reviewedBy = value; }
    /** 返回Version。 */
    public Integer getVersion() { return version; }
    /** 更新Version；调用方仍需遵守所属领域的校验规则。 */
    public void setVersion(Integer value) { this.version = value; }
    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    /** 返回更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** 更新更新时间；调用方仍需遵守所属领域的校验规则。 */
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
