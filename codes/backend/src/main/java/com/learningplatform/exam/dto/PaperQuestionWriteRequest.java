/* 文件职责：定义试卷题目Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 定义试卷题目Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record PaperQuestionWriteRequest(
        @NotNull(message = "题目ID不能为空")
        @Min(value = 1, message = "题目ID必须为正数")
        Long questionId,

        @NotNull(message = "题目顺序不能为空")
        @Min(value = 1, message = "题目顺序必须从1开始")
        Integer sortOrder,

        @NotNull(message = "题目分值不能为空")
        @DecimalMin(value = "0.01", message = "题目分值必须大于0")
        @DecimalMax(value = "999999.99", message = "题目分值不能超过999999.99")
        BigDecimal score
) {
}
