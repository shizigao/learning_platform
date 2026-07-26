package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.offline-teaching.security")
public record TeacherDataSecurityProperties(String encryptionKey) {
    @Override
    public String toString() {
        return "TeacherDataSecurityProperties[encryptionKey=[REDACTED]]";
    }
}
