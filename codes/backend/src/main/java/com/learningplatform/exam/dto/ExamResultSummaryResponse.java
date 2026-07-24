package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamResultSummaryResponse(
        Long id,
        Long examId,
        Long attemptId,
        Long userId,
        BigDecimal totalScore,
        BigDecimal passingScore,
        boolean passed,
        int correctCount,
        int incorrectCount,
        int unansweredCount,
        boolean gradingCompleted,
        LocalDateTime generatedAt
) {
    public static ExamResultSummaryResponse from(ExamResult result) {
        return new ExamResultSummaryResponse(
                result.getId(),
                result.getExamId(),
                result.getAttemptId(),
                result.getUserId(),
                result.getTotalScore(),
                result.getPassingScore(),
                Boolean.TRUE.equals(result.getPassed()),
                result.getCorrectCount(),
                result.getIncorrectCount(),
                result.getUnansweredCount(),
                Boolean.TRUE.equals(result.getGradingCompleted()),
                result.getGeneratedAt()
        );
    }
}
