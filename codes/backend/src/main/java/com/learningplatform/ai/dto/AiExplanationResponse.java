package com.learningplatform.ai.dto;

public record AiExplanationResponse(
        AiTaskResponse task,
        Long conversationId,
        AiMessageResponse question,
        AiMessageResponse answer
) {
}
