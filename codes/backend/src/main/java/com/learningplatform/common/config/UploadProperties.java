package com.learningplatform.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.upload")
public record UploadProperties(
        DataSize maxFileSize,
        int maxFilesPerContent,
        Duration signedUrlTtl
) {
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(200);
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
