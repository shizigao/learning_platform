/* 文件职责：定义AI讲解请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 定义AI讲解请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AiExplanationRequest(
        @Size(max = 64, message = "请求幂等号不能超过64个字符")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]*$",
                message = "请求幂等号只能包含字母、数字、下划线和连字符"
        )
        String requestId,
        @NotBlank(message = "问题不能为空")
        @Size(max = 4000, message = "问题不能超过4000个字符")
        String question
) {
}
