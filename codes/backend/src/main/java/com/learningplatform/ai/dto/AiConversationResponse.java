package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiConversationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AiConversationResponse(
        Long id,
        Long contentId,
        String title,
        AiConversationStatus status,
        LocalDateTime lastMessageAt,
        List<AiMessageResponse> messages,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AiConversationResponse {
        messages = List.copyOf(messages);
    }
}
