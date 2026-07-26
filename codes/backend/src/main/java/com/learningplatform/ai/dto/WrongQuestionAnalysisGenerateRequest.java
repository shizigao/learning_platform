package com.learningplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WrongQuestionAnalysisGenerateRequest(
        @NotBlank(message = "请求幂等号不能为空")
        @Size(max = 64, message = "请求幂等号不能超过64个字符")
        String requestId
) {
}
