package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.UploadProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFileRole;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FileUploadValidator {
    private static final long MEBIBYTE = 1024L * 1024L;
    private static final Map<ContentFileRole, Integer> ROLE_FILE_LIMITS = Map.of(
            ContentFileRole.COVER, 1,
            ContentFileRole.CONTENT, 10,
            ContentFileRole.VIDEO, 5,
            ContentFileRole.ATTACHMENT, 10,
            ContentFileRole.SUBTITLE, 5
    );
    private static final Map<ContentFileRole, Long> ROLE_SIZE_LIMITS = Map.of(
            ContentFileRole.COVER, 10L * MEBIBYTE,
            ContentFileRole.CONTENT, 50L * MEBIBYTE,
            ContentFileRole.VIDEO, 200L * MEBIBYTE,
            ContentFileRole.ATTACHMENT, 200L * MEBIBYTE,
            ContentFileRole.SUBTITLE, 5L * MEBIBYTE
    );
    private static final Map<ContentFileRole, Map<String, Set<String>>> ALLOWED_TYPES = Map.of(
            ContentFileRole.COVER, Map.of(
                    "jpg", Set.of("image/jpeg"),
                    "jpeg", Set.of("image/jpeg"),
                    "png", Set.of("image/png"),
                    "webp", Set.of("image/webp")
            ),
            ContentFileRole.CONTENT, Map.ofEntries(
                    Map.entry("pdf", Set.of("application/pdf")),
                    Map.entry("txt", Set.of("text/plain")),
                    Map.entry("md", Set.of("text/markdown", "text/plain")),
                    Map.entry("doc", Set.of("application/msword")),
                    Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
                    Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
                    Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
                    Map.entry("xls", Set.of("application/vnd.ms-excel")),
                    Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            ),
            ContentFileRole.VIDEO, Map.of(
                    "mp4", Set.of("video/mp4"),
                    "webm", Set.of("video/webm")
            ),
            ContentFileRole.ATTACHMENT, Map.ofEntries(
                    Map.entry("pdf", Set.of("application/pdf")),
                    Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed")),
                    Map.entry("txt", Set.of("text/plain")),
                    Map.entry("doc", Set.of("application/msword")),
                    Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
                    Map.entry("ppt", Set.of("application/vnd.ms-powerpoint")),
                    Map.entry("pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
                    Map.entry("xls", Set.of("application/vnd.ms-excel")),
                    Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            ),
            ContentFileRole.SUBTITLE, Map.of(
                    "srt", Set.of("application/x-subrip", "text/plain"),
                    "vtt", Set.of("text/vtt", "text/plain")
            )
    );

    private final UploadProperties properties;

    public FileUploadValidator(UploadProperties properties) {
        this.properties = properties;
    }

    public ValidatedUploadFile validate(FileUploadValidationRequest request) {
        if (request == null || request.fileRole() == null) {
            throw badRequest("文件用途不能为空");
        }
        validateOwner(request);
        validateCounts(request);

        String filename = normalizeFilename(request.originalFilename());
        String extension = extractExtension(filename);
        String mimeType = normalizeMimeType(request.mimeType());
        validateType(request.fileRole(), extension, mimeType);
        validateSize(request.fileRole(), request.sizeBytes());

        return new ValidatedUploadFile(
                request.fileRole(),
                filename,
                extension,
                mimeType,
                request.sizeBytes()
        );
    }

    private void validateOwner(FileUploadValidationRequest request) {
        if (request.resourceOwnerId() <= 0 || request.requesterUserId() <= 0) {
            throw badRequest("资源所有者或当前用户无效");
        }
        if (!request.requesterAdmin() && request.resourceOwnerId() != request.requesterUserId()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能向其他用户的资源上传文件");
        }
    }

    private void validateCounts(FileUploadValidationRequest request) {
        if (request.existingFileCount() < 0 || request.existingRoleFileCount() < 0) {
            throw badRequest("现有文件数量无效");
        }
        if (request.existingFileCount() >= properties.maxFilesPerContent()) {
            throw badRequest("每份学习资料最多上传 " + properties.maxFilesPerContent() + " 个文件");
        }
        int roleLimit = ROLE_FILE_LIMITS.get(request.fileRole());
        if (request.existingRoleFileCount() >= roleLimit) {
            throw badRequest(request.fileRole().name() + " 类型文件最多上传 " + roleLimit + " 个");
        }
    }

    private String normalizeFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw badRequest("文件名不能为空");
        }
        String filename = originalFilename.trim();
        if (filename.length() > 255 || filename.contains("/") || filename.contains("\\")
                || filename.chars().anyMatch(Character::isISOControl)) {
            throw badRequest("文件名不合法");
        }
        return filename;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == filename.length() - 1) {
            throw badRequest("文件扩展名不能为空");
        }
        String extension = filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!extension.matches("[a-z0-9]{1,10}")) {
            throw badRequest("文件扩展名不合法");
        }
        return extension;
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw badRequest("文件 MIME 类型不能为空");
        }
        return mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
    }

    private void validateType(ContentFileRole role, String extension, String mimeType) {
        Set<String> allowedMimes = ALLOWED_TYPES.get(role).get(extension);
        if (allowedMimes == null || !allowedMimes.contains(mimeType)) {
            throw badRequest("文件扩展名与 MIME 类型不匹配或不受支持");
        }
    }

    private void validateSize(ContentFileRole role, long sizeBytes) {
        if (sizeBytes <= 0) {
            throw badRequest("不能上传空文件");
        }
        long globalLimit = properties.maxFileSize().toBytes();
        long roleLimit = Math.min(globalLimit, ROLE_SIZE_LIMITS.get(role));
        if (sizeBytes > roleLimit) {
            throw badRequest(role.name() + " 类型文件大小不能超过 " + roleLimit / MEBIBYTE + " MB");
        }
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
