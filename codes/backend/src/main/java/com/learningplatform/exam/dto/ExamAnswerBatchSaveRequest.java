package com.learningplatform.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ExamAnswerBatchSaveRequest(
        @NotEmpty(message = "批量保存答案不能为空")
        @Size(max = 200, message = "单次最多保存200道题")
        List<@Valid ExamAnswerBatchItemRequest> answers
) {
    public ExamAnswerBatchSaveRequest {
        answers = answers == null ? List.of() : List.copyOf(answers);
    }
}
