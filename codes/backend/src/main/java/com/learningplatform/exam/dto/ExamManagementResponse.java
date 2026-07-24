package com.learningplatform.exam.dto;

import java.util.List;

public record ExamManagementResponse(
        ExamSummaryResponse exam,
        String instructions,
        ExamPaperSummaryResponse paper,
        List<ExamCandidateResponse> candidates
) {
    public ExamManagementResponse {
        candidates = List.copyOf(candidates);
    }
}
