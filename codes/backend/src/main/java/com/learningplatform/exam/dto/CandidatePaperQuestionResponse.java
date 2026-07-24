package com.learningplatform.exam.dto;

import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.QuestionOptionResponse;

import java.math.BigDecimal;
import java.util.List;

public record CandidatePaperQuestionResponse(
        Long paperQuestionId,
        Long questionId,
        int sortOrder,
        BigDecimal score,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        int blankCount
) {
    public CandidatePaperQuestionResponse {
        options = List.copyOf(options);
    }
}
