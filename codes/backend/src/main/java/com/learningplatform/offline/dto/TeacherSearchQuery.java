/* 文件职责：定义教师搜索查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 定义教师搜索查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class TeacherSearchQuery extends PageQuery {
    @Size(max = 100)
    /** 保存keyword，供该类型的业务逻辑读取或更新。 */
    private String keyword;
    @Size(max = 100)
    /** 保存province，供该类型的业务逻辑读取或更新。 */
    private String province;
    @Size(max = 100)
    /** 保存city，供该类型的业务逻辑读取或更新。 */
    private String city;
    @Size(max = 50)
    /** 保存教学Tag，供该类型的业务逻辑读取或更新。 */
    private String teachingTag;
    @DecimalMin("0.01")
    /** 保存最大Hourly频率，供该类型的业务逻辑读取或更新。 */
    private BigDecimal maxHourlyRate;

    /** 返回Keyword。 */
    public String getKeyword() { return keyword; }
    /** 更新Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setKeyword(String value) { keyword = normalize(value); }
    /** 返回Province。 */
    public String getProvince() { return province; }
    /** 更新Province；调用方仍需遵守所属领域的校验规则。 */
    public void setProvince(String value) { province = normalize(value); }
    /** 返回City。 */
    public String getCity() { return city; }
    /** 更新City；调用方仍需遵守所属领域的校验规则。 */
    public void setCity(String value) { city = normalize(value); }
    /** 返回教学Tag。 */
    public String getTeachingTag() { return teachingTag; }
    /** 更新教学Tag；调用方仍需遵守所属领域的校验规则。 */
    public void setTeachingTag(String value) { teachingTag = normalize(value); }
    /** 返回最大Hourly频率。 */
    public BigDecimal getMaxHourlyRate() { return maxHourlyRate; }
    /** 更新最大Hourly频率；调用方仍需遵守所属领域的校验规则。 */
    public void setMaxHourlyRate(BigDecimal value) { maxHourlyRate = value; }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
