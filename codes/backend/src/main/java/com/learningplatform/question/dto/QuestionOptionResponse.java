package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionOption;

public record QuestionOptionResponse(
        Long id,
        String key,
        String text,
        int sortOrder
) {
    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionKey(),
                option.getOptionText(),
                option.getSortOrder()
        );
    }
}
