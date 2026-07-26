package com.learningplatform.offline.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class TeacherSearchQuery extends PageQuery {
    @Size(max = 100)
    private String keyword;
    @Size(max = 100)
    private String province;
    @Size(max = 100)
    private String city;
    @Size(max = 50)
    private String teachingTag;
    @DecimalMin("0.01")
    private BigDecimal maxHourlyRate;

    public String getKeyword() { return keyword; }
    public void setKeyword(String value) { keyword = normalize(value); }
    public String getProvince() { return province; }
    public void setProvince(String value) { province = normalize(value); }
    public String getCity() { return city; }
    public void setCity(String value) { city = normalize(value); }
    public String getTeachingTag() { return teachingTag; }
    public void setTeachingTag(String value) { teachingTag = normalize(value); }
    public BigDecimal getMaxHourlyRate() { return maxHourlyRate; }
    public void setMaxHourlyRate(BigDecimal value) { maxHourlyRate = value; }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
