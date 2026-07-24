package com.learningplatform.ai.dto;

import jakarta.validation.constraints.Size;

public record AiConversationCreateRequest(
        @Size(max = 200, message = "会话标题不能超过200个字符")
        String title
) {
}
