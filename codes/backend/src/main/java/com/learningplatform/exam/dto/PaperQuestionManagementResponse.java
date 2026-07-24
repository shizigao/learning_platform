package com.learningplatform.exam.dto;

import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.QuestionAnswer;
import com.learningplatform.question.dto.QuestionOptionResponse;

import java.math.BigDecimal;
import java.util.List;

public record PaperQuestionManagementResponse(
        Long id,
        Long questionId,
        int sortOrder,
        BigDecimal score,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        QuestionAnswer answer,
        String analysis
) {
    public PaperQuestionManagementResponse {
        options = List.copyOf(options);
    }
}
