package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.MinioProperties;
import com.learningplatform.common.config.UploadProperties;
import com.learningplatform.common.exception.BusinessException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.Http;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
public class MinioStorageService {
    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final UploadProperties uploadProperties;
    private final FileUploadValidator uploadValidator;
    private final StorageObjectKeyFactory objectKeyFactory;

    public MinioStorageService(
            MinioClient minioClient,
            MinioProperties minioProperties,
            UploadProperties uploadProperties,
            FileUploadValidator uploadValidator,
            StorageObjectKeyFactory objectKeyFactory
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.uploadProperties = uploadProperties;
        this.uploadValidator = uploadValidator;
        this.objectKeyFactory = objectKeyFactory;
    }

    public StoredObject upload(StorageUploadRequest request) {
        if (request == null || request.inputStream() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件内容不能为空");
        }
        ValidatedUploadFile file = uploadValidator.validate(request.toValidationRequest());
        String objectName = objectKeyFactory.create(request.resourceOwnerId(), file.extension());
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .stream(request.inputStream(), file.sizeBytes(), -1L)
                            .contentType(file.mimeType())
                            .build()
            );
            return new StoredObject(
                    minioProperties.bucket(),
                    objectName,
                    file.originalFilename(),
                    file.mimeType(),
                    file.extension(),
                    file.sizeBytes()
            );
        } catch (Exception exception) {
            throw storageFailure("上传", objectName, exception);
        }
    }

    public InputStream download(String objectName, long requesterUserId, boolean requesterAdmin) {
        objectKeyFactory.assertCanAccess(objectName, requesterUserId, requesterAdmin);
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception exception) {
            throw storageFailure("下载", objectName, exception);
        }
    }

    public String createSignedGetUrl(
            String objectName,
            long requesterUserId,
            boolean requesterAdmin,
            Duration ttl
    ) {
        return createSignedUrl(objectName, requesterUserId, requesterAdmin, ttl, Map.of());
    }

    public String createPreviewUrl(String objectName, long requesterUserId, boolean requesterAdmin) {
        return createSignedUrl(
                objectName,
                requesterUserId,
                requesterAdmin,
                uploadProperties.signedUrlTtl(),
                Map.of("response-content-disposition", "inline")
        );
    }

    public String createAuthorizedPreviewUrl(String objectName) {
        objectKeyFactory.ownerId(objectName);
        return createTrustedSignedUrl(
                objectName,
                uploadProperties.signedUrlTtl(),
                Map.of("response-content-disposition", "inline")
        );
    }

    public String createDownloadUrl(
            String objectName,
            String originalFilename,
            long requesterUserId,
            boolean requesterAdmin
    ) {
        String safeFilename = safeDownloadFilename(originalFilename);
        String encodedFilename = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return createSignedUrl(
                objectName,
                requesterUserId,
                requesterAdmin,
                uploadProperties.signedUrlTtl(),
                Map.of(
                        "response-content-disposition",
                        "attachment; filename*=UTF-8''" + encodedFilename
                )
        );
    }

    public String createAuthorizedDownloadUrl(String objectName, String originalFilename) {
        objectKeyFactory.ownerId(objectName);
        String safeFilename = safeDownloadFilename(originalFilename);
        String encodedFilename = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        return createTrustedSignedUrl(
                objectName,
                uploadProperties.signedUrlTtl(),
                Map.of(
                        "response-content-disposition",
                        "attachment; filename*=UTF-8''" + encodedFilename
                )
        );
    }

    public void delete(String objectName, long requesterUserId, boolean requesterAdmin) {
        objectKeyFactory.assertCanAccess(objectName, requesterUserId, requesterAdmin);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception exception) {
            throw storageFailure("删除", objectName, exception);
        }
    }

    private String createSignedUrl(
            String objectName,
            long requesterUserId,
            boolean requesterAdmin,
            Duration ttl,
            Map<String, String> responseHeaders
    ) {
        objectKeyFactory.assertCanAccess(objectName, requesterUserId, requesterAdmin);
        return createTrustedSignedUrl(objectName, ttl, responseHeaders);
    }

    private String createTrustedSignedUrl(
            String objectName,
            Duration ttl,
            Map<String, String> responseHeaders
    ) {
        validateTtl(ttl);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Http.Method.GET)
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .expiry(Math.toIntExact(ttl.toSeconds()))
                            .extraQueryParams(responseHeaders)
                            .build()
            );
        } catch (Exception exception) {
            throw storageFailure("生成签名地址", objectName, exception);
        }
    }

    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()
                || ttl.compareTo(uploadProperties.signedUrlTtl()) > 0) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "签名地址有效期必须大于 0 且不超过 " + uploadProperties.signedUrlTtl().toMinutes() + " 分钟"
            );
        }
    }

    private String safeDownloadFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "download";
        }
        String sanitized = originalFilename
                .replace("\r", "")
                .replace("\n", "")
                .replace("\"", "")
                .trim();
        return sanitized.isBlank() ? "download" : sanitized;
    }

    private BusinessException storageFailure(String operation, String objectName, Exception exception) {
        log.error("MinIO {} failed for object {}", operation, objectName, exception);
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "文件存储服务暂时不可用");
    }
}
