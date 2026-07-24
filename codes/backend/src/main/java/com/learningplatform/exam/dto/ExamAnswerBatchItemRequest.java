package com.learningplatform.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ExamAnswerBatchItemRequest(
        @NotNull(message = "题目ID不能为空")
        @Min(value = 1, message = "题目ID必须为正数")
        Long questionId,

        @NotNull(message = "答案内容不能为空")
        @Valid
        ExamAnswerWriteRequest answer
) {
}
