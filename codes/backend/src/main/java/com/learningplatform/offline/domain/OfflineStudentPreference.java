/* 文件职责：表示线下教学Student学习需求领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示线下教学Student学习需求领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class OfflineStudentPreference {
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存subject，供该类型的业务逻辑读取或更新。 */
    private String subject;
    /** 保存currentLevel，供该类型的业务逻辑读取或更新。 */
    private String currentLevel;
    /** 保存学习Goals，供该类型的业务逻辑读取或更新。 */
    private String learningGoals;
    /** 保存weaknesses，供该类型的业务逻辑读取或更新。 */
    private String weaknesses;
    /** 保存province，供该类型的业务逻辑读取或更新。 */
    private String province;
    /** 保存city，供该类型的业务逻辑读取或更新。 */
    private String city;
    /** 保存district，供该类型的业务逻辑读取或更新。 */
    private String district;
    /** 保存最大Hourly频率，供该类型的业务逻辑读取或更新。 */
    private BigDecimal maxHourlyRate;
    /** 保存availability，供该类型的业务逻辑读取或更新。 */
    private String availability;
    /** 保存教师Preferences，供该类型的业务逻辑读取或更新。 */
    private String teacherPreferences;
    /** 保存additionalNotes，供该类型的业务逻辑读取或更新。 */
    private String additionalNotes;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;
    /** 保存更新时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime updatedAt;

    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long value) { userId = value; }
    /** 返回Subject。 */
    public String getSubject() { return subject; }
    /** 更新Subject；调用方仍需遵守所属领域的校验规则。 */
    public void setSubject(String value) { subject = value; }
    /** 返回CurrentLevel。 */
    public String getCurrentLevel() { return currentLevel; }
    /** 更新CurrentLevel；调用方仍需遵守所属领域的校验规则。 */
    public void setCurrentLevel(String value) { currentLevel = value; }
    /** 返回学习Goals。 */
    public String getLearningGoals() { return learningGoals; }
    /** 更新学习Goals；调用方仍需遵守所属领域的校验规则。 */
    public void setLearningGoals(String value) { learningGoals = value; }
    /** 返回Weaknesses。 */
    public String getWeaknesses() { return weaknesses; }
    /** 更新Weaknesses；调用方仍需遵守所属领域的校验规则。 */
    public void setWeaknesses(String value) { weaknesses = value; }
    /** 返回Province。 */
    public String getProvince() { return province; }
    /** 更新Province；调用方仍需遵守所属领域的校验规则。 */
    public void setProvince(String value) { province = value; }
    /** 返回City。 */
    public String getCity() { return city; }
    /** 更新City；调用方仍需遵守所属领域的校验规则。 */
    public void setCity(String value) { city = value; }
    /** 返回District。 */
    public String getDistrict() { return district; }
    /** 更新District；调用方仍需遵守所属领域的校验规则。 */
    public void setDistrict(String value) { district = value; }
    /** 返回最大Hourly频率。 */
    public BigDecimal getMaxHourlyRate() { return maxHourlyRate; }
    /** 更新最大Hourly频率；调用方仍需遵守所属领域的校验规则。 */
    public void setMaxHourlyRate(BigDecimal value) { maxHourlyRate = value; }
    /** 返回Availability。 */
    public String getAvailability() { return availability; }
    /** 更新Availability；调用方仍需遵守所属领域的校验规则。 */
    public void setAvailability(String value) { availability = value; }
    /** 返回教师Preferences。 */
    public String getTeacherPreferences() { return teacherPreferences; }
    /** 更新教师Preferences；调用方仍需遵守所属领域的校验规则。 */
    public void setTeacherPreferences(String value) { teacherPreferences = value; }
    /** 返回AdditionalNotes。 */
    public String getAdditionalNotes() { return additionalNotes; }
    /** 更新AdditionalNotes；调用方仍需遵守所属领域的校验规则。 */
    public void setAdditionalNotes(String value) { additionalNotes = value; }
    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    /** 返回更新时间。 */
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    /** 更新更新时间；调用方仍需遵守所属领域的校验规则。 */
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
