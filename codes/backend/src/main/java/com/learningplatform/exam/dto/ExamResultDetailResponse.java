package com.learningplatform.exam.dto;

import java.util.List;

public record ExamResultDetailResponse(
        ExamResultSummaryResponse result,
        boolean answersVisible,
        List<ExamResultQuestionResponse> questions
) {
    public ExamResultDetailResponse {
        questions = List.copyOf(questions);
    }
}
