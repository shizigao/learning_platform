package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, DeepSeek deepseek) {
    public record DeepSeek(String baseUrl, String apiKey, String model, Duration timeout) {
    }
}

