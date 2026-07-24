package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiMessage;
import com.learningplatform.ai.domain.AiMessageRole;

import java.time.LocalDateTime;

public record AiMessageResponse(
        Long id,
        Long taskId,
        AiMessageRole role,
        String content,
        Integer sequenceNo,
        Integer tokenCount,
        LocalDateTime createdAt
) {
    public static AiMessageResponse from(AiMessage message) {
        return new AiMessageResponse(
                message.getId(),
                message.getTaskId(),
                message.getRole(),
                message.getContent(),
                message.getSequenceNo(),
                message.getTokenCount(),
                message.getCreatedAt()
        );
    }
}
