package com.learningplatform.ai.dto;

import jakarta.validation.constraints.Size;

public record ExamAiAnalysisGenerateRequest(
        @Size(max = 64, message = "请求幂等号长度不能超过64个字符")
        String requestId
) {
}
