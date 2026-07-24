package com.learningplatform.ai.client;

import java.util.List;

public record AiClientRequest(
        List<AiMessage> messages,
        Integer maxOutputTokens,
        Double temperature,
        AiResponseFormat responseFormat
) {
    public AiClientRequest(
            List<AiMessage> messages,
            Integer maxOutputTokens,
            Double temperature
    ) {
        this(messages, maxOutputTokens, temperature, AiResponseFormat.TEXT);
    }

    public AiClientRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("AI 请求至少需要一条消息");
        }
        messages = List.copyOf(messages);
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            throw new IllegalArgumentException("AI 最大输出长度必须大于0");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("AI temperature 必须在0到2之间");
        }
        if (responseFormat == null) {
            responseFormat = AiResponseFormat.TEXT;
        }
    }
}
