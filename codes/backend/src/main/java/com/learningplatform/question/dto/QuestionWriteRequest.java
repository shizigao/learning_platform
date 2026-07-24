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
