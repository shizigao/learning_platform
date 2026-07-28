/* 文件职责：承载上传配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：配置装配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.upload")
/**
 * 承载上传配置属性配置属性，供配置装配和业务组件以类型安全方式读取。
 *
 * <p>职责边界：只负责组件装配和配置校验，不承载具体业务流程。</p>
 */
public record UploadProperties(
        DataSize maxFileSize,
        int maxFilesPerContent,
        Duration signedUrlTtl
) {
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(200);
    /** 定义 DEFAULT_MAX_FILES_PER_CONTENT 常量，统一该组件使用的固定规则或默认值。 */
    private static final int DEFAULT_MAX_FILES_PER_CONTENT = 20;
    private static final Duration DEFAULT_SIGNED_URL_TTL = Duration.ofMinutes(10);

    public UploadProperties {
        maxFileSize = maxFileSize == null ? DEFAULT_MAX_FILE_SIZE : maxFileSize;
        maxFilesPerContent = maxFilesPerContent <= 0
                ? DEFAULT_MAX_FILES_PER_CONTENT
                : maxFilesPerContent;
        signedUrlTtl = signedUrlTtl == null || signedUrlTtl.isNegative() || signedUrlTtl.isZero()
                ? DEFAULT_SIGNED_URL_TTL
                : signedUrlTtl;
    }
}
