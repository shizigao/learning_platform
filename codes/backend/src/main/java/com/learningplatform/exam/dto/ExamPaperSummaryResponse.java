package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamPaperStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamPaperSummaryResponse(
        Long id,
        Long creatorId,
        String name,
        String description,
        BigDecimal totalScore,
        int questionCount,
        ExamPaperStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamPaperSummaryResponse from(ExamPaper paper) {
        return new ExamPaperSummaryResponse(
                paper.getId(),
                paper.getCreatorId(),
                paper.getName(),
                paper.getDescription(),
                paper.getTotalScore(),
                paper.getQuestionCount(),
                paper.getStatus(),
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }
}
