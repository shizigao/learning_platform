package com.learningplatform.question.dto;

import com.learningplatform.question.domain.Question;
import com.learningplatform.question.domain.QuestionStatus;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record QuestionManagementResponse(
        Long id,
        Long bankId,
        Long creatorId,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        QuestionAnswer answer,
        String analysis,
        BigDecimal defaultScore,
        boolean fillBlankAutoGradable,
        boolean caseSensitive,
        QuestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public QuestionManagementResponse {
        options = List.copyOf(options);
    }
}
