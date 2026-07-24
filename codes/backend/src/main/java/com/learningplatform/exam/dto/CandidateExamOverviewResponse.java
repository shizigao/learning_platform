package com.learningplatform.exam.dto;

public record CandidateExamOverviewResponse(
        ExamSummaryResponse exam,
        String instructions,
        ExamPaperSummaryResponse paper,
        ExamEligibilityResponse eligibility
) {
}
