package com.learningplatform.ai.config;

import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.client.AiClientException;
import com.learningplatform.ai.client.DeepSeekAiClient;
import com.learningplatform.ai.client.MockAiClient;
import com.learningplatform.common.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class AiClientConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            AiClientConfiguration.class
    );

    @Bean
    public AiClient aiClient(AiProperties properties) {
        String provider = properties.provider() == null
                ? "mock"
                : properties.provider().trim().toLowerCase(Locale.ROOT);
        AiClient client = switch (provider) {
            case "", "mock" -> mockClient(properties);
            case "deepseek" -> new DeepSeekAiClient(properties.deepseek());
            default -> throw new AiClientException(
                    AiClientException.Kind.CONFIGURATION,
                    "不支持的 AI_PROVIDER：" + provider
            );
        };
        logActiveConfiguration(client, properties);
        return client;
    }

    private void logActiveConfiguration(
            AiClient client,
            AiProperties properties
    ) {
        AiProperties.DeepSeek deepSeek = properties.deepseek();
        AiProperties.MockProvider mock = properties.mock();
        AiProperties.Limits limits = properties.limits();
        LOGGER.info(
                "AI_CLIENT_CONFIG provider={} model={} mockScenario={} "
                        + "mockDelayMs={} deepSeekBaseUrl={} apiKeyConfigured={} "
                        + "thinking={} connectTimeoutSeconds={} "
                        + "providerTimeoutSeconds={} taskTimeoutSeconds={}",
                client.provider(),
                client.model(),
                mock == null ? "-" : safe(mock.scenario()),
                mock == null || mock.delay() == null
                        ? 0
                        : mock.delay().toMillis(),
                deepSeek == null ? "-" : safeBaseUrl(deepSeek.baseUrl()),
                deepSeek != null
                        && deepSeek.apiKey() != null
                        && !deepSeek.apiKey().isBlank(),
                deepSeek != null && deepSeek.thinkingEnabled(),
                deepSeek == null || deepSeek.connectTimeout() == null
                        ? 0
                        : deepSeek.connectTimeout().toSeconds(),
                deepSeek == null || deepSeek.timeout() == null
                        ? 0
                        : deepSeek.timeout().toSeconds(),
                limits == null || limits.timeout() == null
                        ? 0
                        : limits.timeout().toSeconds()
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String safeBaseUrl(String value) {
        String normalized = safe(value);
        int query = normalized.indexOf('?');
        int fragment = normalized.indexOf('#');
        int end = normalized.length();
        if (query >= 0) {
            end = Math.min(end, query);
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        normalized = normalized.substring(0, end);
        return normalized.replaceFirst("://[^/@]+@", "://");
    }

    private String mockModel(AiProperties properties) {
        return properties.mock() == null
                ? "mock-learning-assistant-v1"
                : properties.mock().model();
    }

    private MockAiClient mockClient(AiProperties properties) {
        AiProperties.MockProvider mock = properties.mock();
        return new MockAiClient(
                mockModel(properties),
                mock == null ? "success" : mock.scenario(),
                mock == null ? java.time.Duration.ZERO : mock.delay()
        );
    }
}
