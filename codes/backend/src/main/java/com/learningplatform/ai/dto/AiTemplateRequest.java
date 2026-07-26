package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiConversationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiTemplateRequest(
        @NotBlank(message = "请求幂等号不能为空")
        @Size(max = 64, message = "请求幂等号不能超过64个字符")
        String requestId,

        @NotNull(message = "请选择对话模板")
        AiConversationTemplate template
) {
}
