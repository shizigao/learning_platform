package com.learningplatform.exam.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

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
