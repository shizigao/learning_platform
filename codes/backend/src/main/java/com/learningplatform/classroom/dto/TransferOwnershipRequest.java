package com.learningplatform.classroom.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TransferOwnershipRequest(
        @NotNull(message = "新拥有者不能为空")
        @Min(value = 1, message = "新拥有者ID必须为正数")
        Long userId
) {
}
