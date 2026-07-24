package com.learningplatform.exam.dto;

import java.util.List;

public record ExamPaperDetailResponse(
        ExamPaperSummaryResponse paper,
        List<PaperQuestionManagementResponse> questions
) {
    public ExamPaperDetailResponse {
        questions = List.copyOf(questions);
    }
}
