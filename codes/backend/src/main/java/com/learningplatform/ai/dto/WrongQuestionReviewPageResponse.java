package com.learningplatform.ai.dto;

import com.learningplatform.exam.dto.WrongReviewExamResponse;

import java.util.List;

public record WrongQuestionReviewPageResponse(
        List<WrongReviewExamResponse> exams,
        int totalQuestionCount,
        int analyzableQuestionCount,
        int quotaRemaining,
        List<WrongQuestionAnalysisResponse> reports
) {
    public WrongQuestionReviewPageResponse {
        exams = List.copyOf(exams);
        reports = List.copyOf(reports);
    }
}
