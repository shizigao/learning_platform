package com.learningplatform.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record QuestionOptionWriteRequest(
        @NotBlank(message = "选项标识不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{1,16}$", message = "选项标识只能包含字母、数字和下划线")
        String key,

        @NotBlank(message = "选项内容不能为空")
        @Size(max = 2000, message = "选项内容不能超过2000个字符")
        String text,

        @Min(value = 0, message = "选项排序值不能小于0")
        Integer sortOrder
) {
}
