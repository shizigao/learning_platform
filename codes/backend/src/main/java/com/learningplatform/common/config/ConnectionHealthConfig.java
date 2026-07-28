/* 文件职责：装配Connection健康检查配置运行配置和依赖组件，并对关键配置项执行启动期校验。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：配置装配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.config;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * 装配Connection健康检查配置运行配置和依赖组件，并对关键配置项执行启动期校验。
 *
 * <p>职责边界：只负责组件装配和配置校验，不承载具体业务流程。</p>
 */
public class ConnectionHealthConfig {

    @Bean
    /** 执行 minioClient 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    /** 执行 minioHealthIndicator 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public HealthIndicator minioHealthIndicator(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        return () -> checkMinio(minioClient, properties.bucket());
    }

    /** 校验Minio及相关业务前置条件，不满足时抛出明确业务异常。 */
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
