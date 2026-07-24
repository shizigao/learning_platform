package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {
    @Override
    public String toString() {
        return "JwtProperties[secret=[REDACTED], accessTokenTtl="
                + accessTokenTtl + "]";
    }
}
