package com.learningplatform.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiSummaryResponse(
        Long id,
        AiTaskResponse task,
        Long contentId,
        String summary,
        List<String> knowledgePoints,
        String reviewOutline,
        String sourceVersion,
        LocalDateTime createdAt
) {
    public AiSummaryResponse {
        knowledgePoints = List.copyOf(knowledgePoints);
    }
}
