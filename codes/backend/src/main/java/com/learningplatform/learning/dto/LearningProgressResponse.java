package com.learningplatform.learning.dto;

import com.learningplatform.learning.domain.LearningProgress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LearningProgressResponse(
        Long id,
        Long contentId,
        LocalDateTime startedAt,
        LocalDateTime lastLearnedAt,
        BigDecimal progressPercent,
        String lastPosition,
        LocalDateTime completedAt
) {
    public static LearningProgressResponse from(LearningProgress progress) {
        return new LearningProgressResponse(
                progress.getId(),
                progress.getContentId(),
                progress.getStartedAt(),
                progress.getLastLearnedAt(),
                progress.getProgressPercent(),
                progress.getLastPosition(),
                progress.getCompletedAt()
        );
    }
}
