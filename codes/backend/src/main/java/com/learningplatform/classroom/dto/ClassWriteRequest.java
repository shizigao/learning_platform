package com.learningplatform.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClassWriteRequest(
        @NotBlank(message = "班级名称不能为空")
        @Size(max = 150, message = "班级名称不能超过150个字符")
        String name,

        @Size(max = 1000, message = "班级介绍不能超过1000个字符")
        String description
) {
}
