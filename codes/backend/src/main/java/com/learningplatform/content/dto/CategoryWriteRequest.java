/* 文件职责：定义分类Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 定义分类Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
