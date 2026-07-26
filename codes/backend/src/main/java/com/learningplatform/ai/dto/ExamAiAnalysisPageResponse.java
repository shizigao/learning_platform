package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.ExamAiAnalysisScope;

import java.util.List;

public record ExamAiAnalysisPageResponse(
        Long examId,
        String examName,
        ExamAiAnalysisScope scope,
        boolean eligible,
        String ineligibleReason,
        int quotaRemaining,
        List<ExamAiAnalysisResponse> reports
) {
    public ExamAiAnalysisPageResponse {
        reports = reports == null ? List.of() : List.copyOf(reports);
    }
}
