package com.learningplatform.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectContentRequest(
        @NotBlank(message = "驳回原因不能为空")
        @Size(max = 1000, message = "驳回原因不能超过1000个字符")
        String reason
) {
}
