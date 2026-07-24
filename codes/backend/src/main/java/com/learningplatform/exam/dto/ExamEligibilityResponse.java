package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamCandidateStatus;

import java.time.LocalDateTime;

public record ExamEligibilityResponse(
        Long examId,
        boolean eligible,
        boolean canStart,
        String reason,
        ExamCandidateStatus candidateStatus,
        LocalDateTime serverTime,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Integer durationMinutes,
        Long attemptId,
        ExamAttemptStatus attemptStatus,
        LocalDateTime startedAt,
        LocalDateTime deadlineAt,
        long remainingSeconds
) {
}
