/* 文件职责：定义AI会话创建请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

import jakarta.validation.constraints.Size;

/**
 * 定义AI会话创建请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AiConversationCreateRequest(
        @Size(max = 200, message = "会话标题不能超过200个字符")
        String title
) {
}
