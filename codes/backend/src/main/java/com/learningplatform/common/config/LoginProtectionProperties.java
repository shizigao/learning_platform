package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.login-protection")
public record LoginProtectionProperties(
        boolean enabled,
        int maxAccountFailures,
        int maxIpFailures,
        Duration window
) {
}
