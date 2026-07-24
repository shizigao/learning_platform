package com.learningplatform.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExamPaperWriteRequest(
        @NotBlank(message = "试卷名称不能为空")
        @Size(max = 200, message = "试卷名称不能超过200个字符")
        String name,

        @Size(max = 1000, message = "试卷说明不能超过1000个字符")
        String description
) {
}
