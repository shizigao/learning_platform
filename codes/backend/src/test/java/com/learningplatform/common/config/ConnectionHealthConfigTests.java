package com.learningplatform.common.config;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConnectionHealthConfigTests {

    private final ConnectionHealthConfig config = new ConnectionHealthConfig();
    private final MinioProperties properties = new MinioProperties(
            "http://localhost:9000",
            "test-access-key",
            "test-secret-key",
            "learning-platform-test"
    );

    @Test
    void reportsUpWhenConfiguredBucketExists() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any())).thenReturn(true);

        HealthIndicator indicator = config.minioHealthIndicator(minioClient, properties);

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    @Test
    void reportsDownWhenConfiguredBucketIsMissing() throws Exception {
        MinioClient minioClient = mock(MinioClient.class);
        when(minioClient.bucketExists(any())).thenReturn(false);

        Health health = config.minioHealthIndicator(minioClient, properties).health();

        assertThat(health.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(health.getDetails())
                .containsEntry("reason", "configured bucket does not exist");
    }
}
