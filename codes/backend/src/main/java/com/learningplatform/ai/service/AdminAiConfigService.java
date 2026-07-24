package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.dto.AdminAiConfigResponse;
import com.learningplatform.common.config.AiProperties;
import org.springframework.stereotype.Service;

@Service
public class AdminAiConfigService {
    private final AiClient aiClient;
    private final AiProperties properties;

    public AdminAiConfigService(AiClient aiClient, AiProperties properties) {
        this.aiClient = aiClient;
        this.properties = properties;
    }

    public AdminAiConfigResponse current() {
        AiProperties.Limits limits = properties.limits();
        AiProperties.DeepSeek deepSeek = properties.deepseek();
        AiProperties.MockProvider mock = properties.mock();
        boolean mockMode = "mock".equalsIgnoreCase(aiClient.provider());
        return new AdminAiConfigResponse(
                aiClient.provider(),
                aiClient.model(),
                mockMode,
                mockMode && mock != null ? safeScenario(mock.scenario()) : null,
                deepSeek != null
                        && deepSeek.apiKey() != null
                        && !deepSeek.apiKey().isBlank(),
                deepSeek == null ? null : deepSeek.baseUrl(),
                deepSeek != null && deepSeek.thinkingEnabled(),
                new AdminAiConfigResponse.Limits(
                        limits.maxInputChars(),
                        limits.maxContextMessages(),
                        limits.maxContextChars(),
                        limits.requestsPerWindow(),
                        limits.rateWindow().toSeconds(),
                        limits.maxConcurrentPerUser(),
                        limits.timeout().toSeconds(),
                        deepSeek == null || deepSeek.connectTimeout() == null
                                ? 0
                                : deepSeek.connectTimeout().toSeconds(),
                        deepSeek == null || deepSeek.timeout() == null
                                ? 0
                                : deepSeek.timeout().toSeconds()
                )
        );
    }

    private String safeScenario(String scenario) {
        return scenario == null || scenario.isBlank()
                ? "success"
                : scenario.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
