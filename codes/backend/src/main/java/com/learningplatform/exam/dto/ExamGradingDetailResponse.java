package com.learningplatform.exam.dto;

import java.util.List;

public record ExamGradingDetailResponse(
        ExamGradingAttemptResponse attempt,
        List<ExamResultQuestionResponse> questions
) {
    public ExamGradingDetailResponse {
        questions = List.copyOf(questions);
    }
}
