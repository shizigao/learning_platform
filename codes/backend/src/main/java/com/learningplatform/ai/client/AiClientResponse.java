/* 文件职责：定义AI 客户端响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

/**
 * 定义AI 客户端响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public record AiClientResponse(
        String provider,
        String model,
        String externalRequestId,
        String content,
        String finishReason,
        int promptTokens,
        int completionTokens,
        int totalTokens
) {
    public AiClientResponse {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("AI 供应商不能为空");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("AI 模型不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI 返回内容不能为空");
        }
        if (promptTokens < 0 || completionTokens < 0 || totalTokens < 0) {
            throw new IllegalArgumentException("AI Token 用量不能为负数");
        }
    }
}
