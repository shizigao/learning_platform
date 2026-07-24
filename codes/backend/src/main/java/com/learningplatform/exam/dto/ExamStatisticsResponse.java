package com.learningplatform.exam.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExamStatisticsResponse(
        Long examId,
        int totalCandidates,
        int participatedCount,
        int submittedCount,
        int notParticipatedCount,
        int gradedCount,
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        int passedCount,
        BigDecimal passRate,
        List<ExamQuestionStatisticsResponse> questions
) {
    public ExamStatisticsResponse {
        questions = List.copyOf(questions);
    }
}
