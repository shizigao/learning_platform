package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAnswerGradingStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ExamAnswerResponse(
        Long id,
        Long questionId,
        Long paperQuestionId,
        List<String> values,
        String text,
        ExamAnswerGradingStatus gradingStatus,
        LocalDateTime savedAt
) {
    public ExamAnswerResponse {
        values = List.copyOf(values);
    }
}
