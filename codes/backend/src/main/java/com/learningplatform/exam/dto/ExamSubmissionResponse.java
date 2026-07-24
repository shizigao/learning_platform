package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamSubmissionType;

import java.time.LocalDateTime;

public record ExamSubmissionResponse(
        Long attemptId,
        ExamAttemptStatus status,
        LocalDateTime submittedAt,
        ExamSubmissionType submissionType,
        int answeredCount,
        int totalQuestions
) {
}
