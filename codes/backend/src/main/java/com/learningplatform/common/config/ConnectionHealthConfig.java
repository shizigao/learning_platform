package com.learningplatform.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectionHealthConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public HealthIndicator minioHealthIndicator(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        return () -> checkMinio(minioClient, properties.bucket());
    }

    private Health checkMinio(MinioClient minioClient, String bucket) {
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!bucketExists) {
                return Health.down()
                        .withDetail("reason", "configured bucket does not exist")
                        .build();
            }
            return Health.up().build();
        } catch (Exception exception) {
            return Health.down(exception).build();
        }
    }
}
