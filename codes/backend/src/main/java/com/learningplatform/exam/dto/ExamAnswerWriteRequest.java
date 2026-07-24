package com.learningplatform.exam.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

public record ExamAnswerWriteRequest(
        @Size(max = 50, message = "结构化答案数量不能超过50项")
        List<@Size(max = 2000, message = "单项答案不能超过2000个字符") String> values,

        @Size(max = 20000, message = "文本答案不能超过20000个字符")
        String text
) {
    public ExamAnswerWriteRequest {
        values = values == null ? List.of() : List.copyOf(values);
    }
}
