package com.learningplatform.ai.dto;

public record AdminAiConfigResponse(
        String provider,
        String model,
        boolean mockMode,
        String mockScenario,
        boolean apiKeyConfigured,
        String baseUrl,
        boolean thinkingEnabled,
        Limits limits
) {
    public record Limits(
            int maxInputChars,
            int maxContextMessages,
            int maxContextChars,
            int requestsPerWindow,
            long rateWindowSeconds,
            int maxConcurrentPerUser,
            long requestTimeoutSeconds,
            long providerConnectTimeoutSeconds,
            long providerTimeoutSeconds
    ) {
    }
}
