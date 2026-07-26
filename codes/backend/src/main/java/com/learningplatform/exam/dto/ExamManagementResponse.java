package com.learningplatform.exam.dto;

import java.util.List;

public record ExamManagementResponse(
        ExamSummaryResponse exam,
        String instructions,
        ExamPaperSummaryResponse paper,
        List<ExamCandidateResponse> candidates,
        List<Long> classIds
) {
    public ExamManagementResponse {
        candidates = List.copyOf(candidates);
        classIds = List.copyOf(classIds);
    }
}
