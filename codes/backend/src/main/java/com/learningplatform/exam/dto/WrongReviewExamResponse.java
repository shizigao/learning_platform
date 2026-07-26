package com.learningplatform.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WrongReviewExamResponse(
        Long resultId,
        Long examId,
        String examName,
        BigDecimal fullScore,
        BigDecimal totalScore,
        BigDecimal passingScore,
        boolean passed,
        boolean answersVisible,
        LocalDateTime generatedAt,
        List<ExamResultQuestionResponse> questions
) {
    public WrongReviewExamResponse {
        questions = List.copyOf(questions);
    }
}
