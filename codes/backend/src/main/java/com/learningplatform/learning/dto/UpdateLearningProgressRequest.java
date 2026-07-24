package com.learningplatform.learning.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateLearningProgressRequest(
        @NotNull(message = "学习进度不能为空")
        @DecimalMin(value = "0.00", message = "学习进度不能小于0")
        @DecimalMax(value = "100.00", message = "学习进度不能超过100")
        BigDecimal progressPercent,

        @Size(max = 255, message = "学习位置不能超过255个字符")
        String lastPosition
) {
}
