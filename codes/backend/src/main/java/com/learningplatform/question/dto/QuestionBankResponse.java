package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionBank;
import com.learningplatform.question.domain.QuestionStatus;

import java.time.LocalDateTime;

public record QuestionBankResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        QuestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QuestionBankResponse from(QuestionBank bank) {
        return new QuestionBankResponse(
                bank.getId(),
                bank.getOwnerId(),
                bank.getName(),
                bank.getDescription(),
                bank.getStatus(),
                bank.getCreatedAt(),
                bank.getUpdatedAt()
        );
    }
}
