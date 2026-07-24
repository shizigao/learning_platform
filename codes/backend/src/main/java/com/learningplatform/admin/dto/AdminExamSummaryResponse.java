package com.learningplatform.admin.dto;

import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.user.domain.User;

public record AdminExamSummaryResponse(
        ExamSummaryResponse exam,
        String publisherUsername,
        String publisherNickname
) {
    public static AdminExamSummaryResponse from(
            ExamSummaryResponse exam,
            User publisher
    ) {
        return new AdminExamSummaryResponse(
                exam,
                publisher.getUsername(),
                publisher.getNickname()
        );
    }
}
