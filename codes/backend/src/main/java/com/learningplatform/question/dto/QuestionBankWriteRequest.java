package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QuestionBankWriteRequest(
        @NotBlank(message = "题库名称不能为空")
        @Size(max = 150, message = "题库名称不能超过150个字符")
        String name,

        @Size(max = 1000, message = "题库描述不能超过1000个字符")
        String description,

        QuestionStatus status
) {
}
