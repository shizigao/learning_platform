package com.learningplatform.common.config;

import com.learningplatform.auth.dto.LoginRequest;
import com.learningplatform.auth.dto.LoginResponse;
import com.learningplatform.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveConfigurationRedactionTests {
    @Test
    void redactsAuthenticationSecretsFromStringRepresentations() {
        LoginRequest login = new LoginRequest("test_user", "PlainPassword123");
        RegisterRequest register = new RegisterRequest(
                "test_user",
                "PlainPassword123",
                "测试用户",
                null,
                null
        );
        LoginResponse response = new LoginResponse(
                "test-jwt-token-value",
                "Bearer",
                7200,
                null
        );

        assertThat(login.toString())
                .contains("[REDACTED]")
                .doesNotContain("PlainPassword123");
        assertThat(register.toString())
                .contains("[REDACTED]")
                .doesNotContain("PlainPassword123");
        assertThat(response.toString())
                .contains("[REDACTED]")
                .doesNotContain("test-jwt-token-value");
    }

    @Test
    void redactsConfigurationSecretsFromStringRepresentations() {
        AiProperties.DeepSeek deepSeek = new AiProperties.DeepSeek(
                "https://api.deepseek.com",
                "test-deepseek-secret",
                "deepseek-chat",
                Duration.ofSeconds(10),
                Duration.ofSeconds(90),
                false
        );
        JwtProperties jwt = new JwtProperties(
                "test-jwt-secret",
                Duration.ofHours(2)
        );
        MinioProperties minio = new MinioProperties(
                "http://localhost:9000",
                "test-minio-access",
                "test-minio-secret",
                "learning-platform"
        );
        TeacherDataSecurityProperties teacherData =
                new TeacherDataSecurityProperties("teacher-secret-key");

        assertThat(deepSeek.toString())
                .contains("[REDACTED]")
                .doesNotContain("test-deepseek-secret");
        assertThat(jwt.toString())
                .contains("[REDACTED]")
                .doesNotContain("test-jwt-secret");
        assertThat(minio.toString())
                .contains("[REDACTED]")
                .doesNotContain("test-minio-access", "test-minio-secret");
        assertThat(teacherData.toString())
                .contains("[REDACTED]")
                .doesNotContain("teacher-secret-key");
    }
}
