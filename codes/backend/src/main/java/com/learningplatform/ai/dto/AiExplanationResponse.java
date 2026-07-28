/* 文件职责：定义AI讲解响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

/**
 * 定义AI讲解响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AiExplanationResponse(
        AiTaskResponse task,
        Long conversationId,
        AiMessageResponse question,
        AiMessageResponse answer
) {
}
