package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        String provider,
        MockProvider mock,
        DeepSeek deepseek,
        Limits limits
) {
    public record MockProvider(String model, String scenario, Duration delay) {
    }

    public record DeepSeek(
            String baseUrl,
            String apiKey,
            String model,
            Duration connectTimeout,
            Duration timeout,
            boolean thinkingEnabled
    ) {
        @Override
        public String toString() {
            return "DeepSeek[baseUrl=" + baseUrl
                    + ", apiKey=[REDACTED], model=" + model
                    + ", connectTimeout=" + connectTimeout
                    + ", timeout=" + timeout
                    + ", thinkingEnabled=" + thinkingEnabled + "]";
        }
    }

    public record Limits(
            int maxInputChars,
            int maxContextMessages,
            int maxContextChars,
            int requestsPerWindow,
            Duration rateWindow,
            int maxConcurrentPerUser,
            Duration timeout
    ) {
    }
}
