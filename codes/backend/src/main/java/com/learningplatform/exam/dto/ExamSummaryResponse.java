package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamSummaryResponse(
        Long id,
        Long publisherId,
        Long paperId,
        String name,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int durationMinutes,
        BigDecimal passingScore,
        boolean showResultImmediately,
        boolean showAnswerAfterFinish,
        ExamStatus status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExamSummaryResponse from(Exam exam) {
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getPublisherId(),
                exam.getPaperId(),
                exam.getName(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes(),
                exam.getPassingScore(),
                Boolean.TRUE.equals(exam.getShowResultImmediately()),
                Boolean.TRUE.equals(exam.getShowAnswerAfterFinish()),
                exam.getStatus(),
                exam.getPublishedAt(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
