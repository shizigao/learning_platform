package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamSubmissionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ExamGradingAttemptResponse(
        Long attemptId,
        Long userId,
        String username,
        String nickname,
        ExamAttemptStatus status,
        LocalDateTime submittedAt,
        ExamSubmissionType submissionType,
        int pendingReviewCount,
        BigDecimal totalScore,
        boolean gradingCompleted
) {
    public static ExamGradingAttemptResponse from(ExamAttempt attempt) {
        return new ExamGradingAttemptResponse(
                attempt.getId(),
                attempt.getUserId(),
                attempt.getUsername(),
                attempt.getNickname(),
                attempt.getStatus(),
                attempt.getSubmittedAt(),
                attempt.getSubmissionType(),
                attempt.getPendingReviewCount() == null ? 0 : attempt.getPendingReviewCount(),
                attempt.getFinalScore(),
                Boolean.TRUE.equals(attempt.getGradingCompleted())
        );
    }
}
