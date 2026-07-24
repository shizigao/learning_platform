package com.learningplatform.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryWriteRequest(
        @Min(value = 1, message = "父分类ID必须为正数")
        Long parentId,

        @NotBlank(message = "分类名称不能为空")
        @Size(max = 100, message = "分类名称不能超过100个字符")
        String name,

        @NotBlank(message = "分类标识不能为空")
        @Size(max = 100, message = "分类标识不能超过100个字符")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "分类标识只能包含小写字母、数字和连字符")
        String slug,

        @Size(max = 500, message = "分类描述不能超过500个字符")
        String description,

        @Min(value = 0, message = "排序值不能小于0")
        Integer sortOrder,

        Boolean enabled
) {
}
