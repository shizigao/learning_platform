/* 文件职责：定义Manual评分请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 定义Manual评分请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ManualGradeRequest(
        @NotNull(message = "得分不能为空")
        @DecimalMin(value = "0.00", message = "得分不能小于0")
        @Digits(integer = 6, fraction = 2, message = "得分最多保留2位小数")
        BigDecimal score,

        @Size(max = 2000, message = "批改评语不能超过2000个字符")
        String comment
) {
}
