package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAttemptStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ExamStartResponse(
        Long attemptId,
        ExamAttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime deadlineAt,
        LocalDateTime serverTime,
        long remainingSeconds,
        ExamSummaryResponse exam,
        String instructions,
        ExamPaperSummaryResponse paper,
        List<CandidatePaperQuestionResponse> questions,
        List<ExamAnswerResponse> answers
) {
    public ExamStartResponse {
        questions = List.copyOf(questions);
        answers = List.copyOf(answers);
    }
}
