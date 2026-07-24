package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskStatus;
import com.learningplatform.ai.domain.AiTaskType;

import java.time.LocalDateTime;

public record AiTaskResponse(
        Long id,
        String requestId,
        Long contentId,
        Long conversationId,
        AiTaskType taskType,
        String provider,
        String model,
        AiTaskStatus status,
        Integer inputChars,
        Integer quotaCost,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
    public static AiTaskResponse from(AiTask task) {
        return new AiTaskResponse(
                task.getId(),
                task.getRequestId(),
                task.getContentId(),
                task.getConversationId(),
                task.getTaskType(),
                task.getProvider(),
                task.getModel(),
                task.getStatus(),
                task.getInputChars(),
                task.getQuotaCost(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt()
        );
    }
}
