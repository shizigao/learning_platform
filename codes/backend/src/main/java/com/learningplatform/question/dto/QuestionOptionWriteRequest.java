/* 文件职责：定义题目选项Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 定义题目选项Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
