package com.learningplatform.exam.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ManualGradeRequest(
        @NotNull(message = "得分不能为空")
        @DecimalMin(value = "0.00", message = "得分不能小于0")
        @Digits(integer = 6, fraction = 2, message = "得分最多保留2位小数")
        BigDecimal score,

        @Size(max = 2000, message = "批改评语不能超过2000个字符")
        String comment
) {
}
