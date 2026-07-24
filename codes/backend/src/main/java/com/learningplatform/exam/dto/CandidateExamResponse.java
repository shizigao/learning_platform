package com.learningplatform.exam.dto;

import java.util.List;

public record CandidateExamResponse(
        ExamSummaryResponse exam,
        String instructions,
        ExamPaperSummaryResponse paper,
        List<CandidatePaperQuestionResponse> questions
) {
    public CandidateExamResponse {
        questions = List.copyOf(questions);
    }
}
