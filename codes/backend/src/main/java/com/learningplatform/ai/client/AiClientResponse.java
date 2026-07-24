package com.learningplatform.ai.client;

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
