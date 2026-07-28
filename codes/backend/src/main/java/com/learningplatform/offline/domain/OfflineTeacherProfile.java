/* 文件职责：表示线下教学教师资料领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示线下教学教师资料领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class OfflineTeacherProfile {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存来源申请ID，供该类型的业务逻辑读取或更新。 */
    private Long sourceApplicationId;
    /** 保存教师名称，供该类型的业务逻辑读取或更新。 */
    private String teacherName;
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
    private TeacherProfileStatus status;
    /** 保存suspended原因，供该类型的业务逻辑读取或更新。 */
    private String suspendedReason;
    /** 保存approved时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime approvedAt;
    /** 保存approved按，供该类型的业务逻辑读取或更新。 */
    private Long approvedBy;
    /** 保存username，供该类型的业务逻辑读取或更新。 */
    private String username;
    /** 保存nickname，供该类型的业务逻辑读取或更新。 */
    private String nickname;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;
    /** 保存更新时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime updatedAt;

    /** 返回ID。 */
    public Long getId() { return id; }
    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long value) { this.id = value; }
    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long value) { this.userId = value; }
    /** 返回来源申请ID。 */
    public Long getSourceApplicationId() { return sourceApplicationId; }
    /** 更新来源申请ID；调用方仍需遵守所属领域的校验规则。 */
    public void setSourceApplicationId(Long value) { this.sourceApplicationId = value; }
    /** 返回教师名称。 */
    public String getTeacherName() { return teacherName; }
    /** 更新教师名称；调用方仍需遵守所属领域的校验规则。 */
    public void setTeacherName(String value) { this.teacherName = value; }
    /** 返回Gender。 */
    public String getGender() { return gender; }
    /** 更新Gender；调用方仍需遵守所属领域的校验规则。 */
    public void setGender(String value) { this.gender = value; }
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
    public void setInstitution(String value) { this.institution = value; }
    /** 返回Province。 */
    public String getProvince() { return province; }
    /** 更新Province；调用方仍需遵守所属领域的校验规则。 */
    public void setProvince(String value) { this.province = value; }
    /** 返回City。 */
    public String getCity() { return city; }
    /** 更新City；调用方仍需遵守所属领域的校验规则。 */
    public void setCity(String value) { this.city = value; }
    /** 返回District。 */
    public String getDistrict() { return district; }
    /** 更新District；调用方仍需遵守所属领域的校验规则。 */
    public void setDistrict(String value) { this.district = value; }
    /** 返回Bio。 */
    public String getBio() { return bio; }
    /** 更新Bio；调用方仍需遵守所属领域的校验规则。 */
    public void setBio(String value) { this.bio = value; }
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
    public TeacherProfileStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(TeacherProfileStatus value) { this.status = value; }
    /** 返回Suspended原因。 */
    public String getSuspendedReason() { return suspendedReason; }
    /** 更新Suspended原因；调用方仍需遵守所属领域的校验规则。 */
    public void setSuspendedReason(String value) { this.suspendedReason = value; }
    /** 返回Approved时间。 */
    public LocalDateTime getApprovedAt() { return approvedAt; }
    /** 更新Approved时间；调用方仍需遵守所属领域的校验规则。 */
    public void setApprovedAt(LocalDateTime value) { this.approvedAt = value; }
    /** 返回Approved按。 */
    public Long getApprovedBy() { return approvedBy; }
    /** 更新Approved按；调用方仍需遵守所属领域的校验规则。 */
    public void setApprovedBy(Long value) { this.approvedBy = value; }
    /** 返回Username。 */
    public String getUsername() { return username; }
    /** 更新Username；调用方仍需遵守所属领域的校验规则。 */
    public void setUsername(String value) { this.username = value; }
    /** 返回Nickname。 */
    public String getNickname() { return nickname; }
    /** 更新Nickname；调用方仍需遵守所属领域的校验规则。 */
    public void setNickname(String value) { this.nickname = value; }
    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    /** 返回更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** 更新更新时间；调用方仍需遵守所属领域的校验规则。 */
    public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
