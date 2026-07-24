package com.learningplatform.exam.dto;

import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;

public record ExamQuestionStatisticsResponse(
        Long questionId,
        int sortOrder,
        QuestionType questionType,
        String stem,
        BigDecimal maxScore,
        int gradedCount,
        int answeredCount,
        int correctCount,
        BigDecimal correctRate
) {
}
