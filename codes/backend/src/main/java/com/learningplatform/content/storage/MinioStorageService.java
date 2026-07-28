/* 文件职责：实现Minio存储业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：对象存储层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
import java.io.BufferedInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Service
/**
 * 实现Minio存储业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：对象存储保持私有，外部访问只能使用受控的短期签名地址。</p>
 */
public class MinioStorageService {
    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    /** 通过minioClient调用隔离后的外部能力。 */
    private final MinioClient minioClient;
    /** 保存minio配置属性，供该类型的业务逻辑读取或更新。 */
    private final MinioProperties minioProperties;
    /** 保存上传配置属性，供该类型的业务逻辑读取或更新。 */
    private final UploadProperties uploadProperties;
    /** 保存上传校验器，供该类型的业务逻辑读取或更新。 */
    private final FileUploadValidator uploadValidator;
    /** 保存signature校验器，供该类型的业务逻辑读取或更新。 */
    private final FileContentSignatureValidator signatureValidator;
    /** 保存object键工厂，供该类型的业务逻辑读取或更新。 */
    private final StorageObjectKeyFactory objectKeyFactory;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public MinioStorageService(
            MinioClient minioClient,
            MinioProperties minioProperties,
            UploadProperties uploadProperties,
            FileUploadValidator uploadValidator,
            FileContentSignatureValidator signatureValidator,
            StorageObjectKeyFactory objectKeyFactory
    ) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
        this.uploadProperties = uploadProperties;
        this.uploadValidator = uploadValidator;
        this.signatureValidator = signatureValidator;
        this.objectKeyFactory = objectKeyFactory;
    }

    /** 执行 upload 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public StoredObject upload(StorageUploadRequest request) {
        if (request == null || request.inputStream() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "上传文件内容不能为空");
        }
        ValidatedUploadFile file = uploadValidator.validate(request.toValidationRequest());
        String objectName = objectKeyFactory.create(request.resourceOwnerId(), file.extension());
        try (BufferedInputStream input =
                     new BufferedInputStream(request.inputStream())) {
            input.mark(512);
            byte[] header = input.readNBytes(512);
            input.reset();
            signatureValidator.validate(file.extension(), header);
            // 这里就是minio的连接处
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectName)
                            .stream(input, file.sizeBytes(), -1L)
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

    /** 执行 download 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public InputStream download(String objectName, long requesterUserId, boolean requesterAdmin) {
        objectKeyFactory.assertCanAccess(objectName, requesterUserId, requesterAdmin);
        return getObject(objectName);
    }

    /** 执行 downloadAuthorized 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public InputStream downloadAuthorized(String objectName) {
        objectKeyFactory.ownerId(objectName);
        return getObject(objectName);
    }

    /** 返回Object。 */
    private InputStream getObject(String objectName) {
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

    /** 创建或初始化SignedGetUrl，并维护唯一性、初始状态和必要关联。 */
    public String createSignedGetUrl(
            String objectName,
            long requesterUserId,
            boolean requesterAdmin,
            Duration ttl
    ) {
        return createSignedUrl(objectName, requesterUserId, requesterAdmin, ttl, Map.of());
    }

    /** 创建或初始化PreviewUrl，并维护唯一性、初始状态和必要关联。 */
    public String createPreviewUrl(String objectName, long requesterUserId, boolean requesterAdmin) {
        return createSignedUrl(
                objectName,
                requesterUserId,
                requesterAdmin,
                uploadProperties.signedUrlTtl(),
                Map.of("response-content-disposition", "inline")
        );
    }

    /** 创建或初始化AuthorizedPreviewUrl，并维护唯一性、初始状态和必要关联。 */
    public String createAuthorizedPreviewUrl(String objectName) {
        objectKeyFactory.ownerId(objectName);
        return createTrustedSignedUrl(
                objectName,
                uploadProperties.signedUrlTtl(),
                Map.of("response-content-disposition", "inline")
        );
    }

    /** 创建或初始化DownloadUrl，并维护唯一性、初始状态和必要关联。 */
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

    /** 创建或初始化AuthorizedDownloadUrl，并维护唯一性、初始状态和必要关联。 */
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

    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
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

    /** 创建或初始化SignedUrl，并维护唯一性、初始状态和必要关联。 */
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

    /** 创建或初始化TrustedSignedUrl，并维护唯一性、初始状态和必要关联。 */
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

    /** 校验Ttl及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()
                || ttl.compareTo(uploadProperties.signedUrlTtl()) > 0) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "签名地址有效期必须大于 0 且不超过 " + uploadProperties.signedUrlTtl().toMinutes() + " 分钟"
            );
        }
    }

    /** 执行 safeDownloadFilename 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 执行 storageFailure 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private BusinessException storageFailure(String operation, String objectName, Exception exception) {
        log.error("MinIO {} failed for object {}", operation, objectName, exception);
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "文件存储服务暂时不可用");
    }
}
