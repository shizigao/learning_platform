package com.learningplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiExplanationRequest(
        @Size(max = 64, message = "请求幂等号不能超过64个字符")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]*$",
                message = "请求幂等号只能包含字母、数字、下划线和连字符"
        )
        String requestId,
        @NotBlank(message = "问题不能为空")
        @Size(max = 4000, message = "问题不能超过4000个字符")
        String question
) {
}
