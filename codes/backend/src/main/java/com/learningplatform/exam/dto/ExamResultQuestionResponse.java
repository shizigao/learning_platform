package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAnswerGradingStatus;
import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.QuestionAnswer;
import com.learningplatform.question.dto.QuestionOptionResponse;

import java.math.BigDecimal;
import java.util.List;

public record ExamResultQuestionResponse(
        Long answerId,
        Long questionId,
        int sortOrder,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        BigDecimal maxScore,
        List<String> values,
        String text,
        BigDecimal score,
        Boolean correct,
        ExamAnswerGradingStatus gradingStatus,
        QuestionAnswer correctAnswer,
        String analysis,
        String graderComment
) {
    public ExamResultQuestionResponse {
        options = List.copyOf(options);
        values = List.copyOf(values);
    }
}
