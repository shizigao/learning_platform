/* 文件职责：定义题目Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 定义题目Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record QuestionWriteRequest(
        @NotNull(message = "题库ID不能为空")
        Long bankId,

        @NotNull(message = "题型不能为空")
        QuestionType questionType,

        @NotBlank(message = "题干不能为空")
        @Size(max = 10000, message = "题干不能超过10000个字符")
        String stem,

        @Valid
        @Size(max = 10, message = "选项不能超过10个")
        List<@NotNull(message = "选项不能为空") @Valid QuestionOptionWriteRequest> options,

        @NotNull(message = "正确答案不能为空")
        QuestionAnswer answer,

        @Size(max = 10000, message = "答案解析不能超过10000个字符")
        String analysis,

        @NotNull(message = "默认分值不能为空")
        @DecimalMin(value = "0.01", message = "默认分值必须大于0")
        @DecimalMax(value = "999999.99", message = "默认分值不能超过999999.99")
        BigDecimal defaultScore,

        Boolean fillBlankAutoGradable,

        Boolean caseSensitive
) {
}
