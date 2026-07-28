/* 文件职责：定义题目题库Write请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义题目题库Write请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record QuestionBankWriteRequest(
        @NotBlank(message = "题库名称不能为空")
        @Size(max = 150, message = "题库名称不能超过150个字符")
        String name,

        @Size(max = 1000, message = "题库描述不能超过1000个字符")
        String description,

        QuestionStatus status
) {
}
