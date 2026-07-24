package com.learningplatform.learning.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @Min(value = 1, message = "父评论ID必须为正数")
        Long parentId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 2000, message = "评论内容不能超过2000个字符")
        String body
) {
}
