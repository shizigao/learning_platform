package com.learningplatform.exam.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ReplacePaperQuestionsRequest(
        @NotEmpty(message = "试卷至少需要一道题")
        @Size(max = 500, message = "单份试卷不能超过500道题")
        List<@NotNull(message = "试卷题目不能为空") @Valid PaperQuestionWriteRequest> questions
) {
}
