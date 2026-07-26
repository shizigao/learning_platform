package com.learningplatform.offline.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfflineStudentPreference {
    private Long userId;
    private String subject;
    private String currentLevel;
    private String learningGoals;
    private String weaknesses;
    private String province;
    private String city;
    private String district;
    private BigDecimal maxHourlyRate;
    private String availability;
    private String teacherPreferences;
    private String additionalNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String value) { currentLevel = value; }
    public String getLearningGoals() { return learningGoals; }
    public void setLearningGoals(String value) { learningGoals = value; }
    public String getWeaknesses() { return weaknesses; }
    public void setWeaknesses(String value) { weaknesses = value; }
    public String getProvince() { return province; }
    public void setProvince(String value) { province = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getDistrict() { return district; }
    public void setDistrict(String value) { district = value; }
    public BigDecimal getMaxHourlyRate() { return maxHourlyRate; }
    public void setMaxHourlyRate(BigDecimal value) { maxHourlyRate = value; }
    public String getAvailability() { return availability; }
    public void setAvailability(String value) { availability = value; }
    public String getTeacherPreferences() { return teacherPreferences; }
    public void setTeacherPreferences(String value) { teacherPreferences = value; }
    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String value) { additionalNotes = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
