package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public record ApiRateLimitProperties(
        boolean enabled,
        int generalRequestsPerMinute,
        int authRequestsPerMinute,
        int uploadRequestsPerMinute
) {
    private static final int DEFAULT_GENERAL_LIMIT = 600;
    private static final int DEFAULT_AUTH_LIMIT = 30;
    private static final int DEFAULT_UPLOAD_LIMIT = 30;

    public ApiRateLimitProperties {
        generalRequestsPerMinute = positiveOrDefault(
                generalRequestsPerMinute,
                DEFAULT_GENERAL_LIMIT
        );
        authRequestsPerMinute = positiveOrDefault(
                authRequestsPerMinute,
                DEFAULT_AUTH_LIMIT
        );
        uploadRequestsPerMinute = positiveOrDefault(
                uploadRequestsPerMinute,
                DEFAULT_UPLOAD_LIMIT
        );
    }

    private static int positiveOrDefault(int value, int fallback) {
        return value > 0 ? value : fallback;
    }
}
