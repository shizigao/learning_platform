package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.UploadProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFileRole;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUploadValidatorTests {
    private final FileUploadValidator validator = new FileUploadValidator(
            new UploadProperties(DataSize.ofMegabytes(200), 20, Duration.ofMinutes(10))
    );

    @Test
    void acceptsMatchingExtensionMimeSizeCountAndOwner() {
        ValidatedUploadFile file = validator.validate(request(
                ContentFileRole.CONTENT,
                "学习资料.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                1024,
                2,
                1,
                7,
                7,
                false
        ));

        assertThat(file.originalFilename()).isEqualTo("学习资料.docx");
        assertThat(file.extension()).isEqualTo("docx");
    }

    @Test
    void rejectsExtensionAndMimeMismatch() {
        assertBadRequest(request(
                ContentFileRole.COVER,
                "cover.jpg",
                "image/png",
                1024,
                0,
                0,
                7,
                7,
                false
        ), "扩展名与 MIME 类型不匹配");
    }

    @Test
    void rejectsRoleSpecificSizeLimit() {
        assertBadRequest(request(
                ContentFileRole.COVER,
                "cover.png",
                "image/png",
                10L * 1024 * 1024 + 1,
                0,
                0,
                7,
                7,
                false
        ), "不能超过 10 MB");
    }

    @Test
    void rejectsTotalAndRoleFileLimits() {
        assertBadRequest(request(
                ContentFileRole.ATTACHMENT,
                "archive.zip",
                "application/zip",
                1024,
                20,
                0,
                7,
                7,
                false
        ), "最多上传 20 个文件");

        assertBadRequest(request(
                ContentFileRole.COVER,
                "cover.webp",
                "image/webp",
                1024,
                1,
                1,
                7,
                7,
                false
        ), "最多上传 1 个");
    }

    @Test
    void rejectsCrossOwnerUploadAndAllowsAdminOverride() {
        FileUploadValidationRequest crossOwnerRequest = request(
                ContentFileRole.VIDEO,
                "lesson.mp4",
                "video/mp4",
                1024,
                0,
                0,
                7,
                8,
                false
        );

        assertThatThrownBy(() -> validator.validate(crossOwnerRequest))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        ValidatedUploadFile file = validator.validate(request(
                ContentFileRole.VIDEO,
                "lesson.mp4",
                "video/mp4",
                1024,
                0,
                0,
                7,
                8,
                true
        ));
        assertThat(file.extension()).isEqualTo("mp4");
    }

    @Test
    void rejectsPathLikeAndUnsupportedFilenames() {
        assertBadRequest(request(
                ContentFileRole.CONTENT,
                "../lesson.pdf",
                "application/pdf",
                1024,
                0,
                0,
                7,
                7,
                false
        ), "文件名不合法");

        assertBadRequest(request(
                ContentFileRole.ATTACHMENT,
                "script.exe",
                "application/octet-stream",
                1024,
                0,
                0,
                7,
                7,
                false
        ), "不受支持");
    }

    private FileUploadValidationRequest request(
            ContentFileRole role,
            String filename,
            String mimeType,
            long size,
            int existingCount,
            int existingRoleCount,
            long ownerId,
            long requesterId,
            boolean admin
    ) {
        return new FileUploadValidationRequest(
                role,
                filename,
                mimeType,
                size,
                existingCount,
                existingRoleCount,
                ownerId,
                requesterId,
                admin
        );
    }

    private void assertBadRequest(FileUploadValidationRequest request, String messagePart) {
        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
                    assertThat(exception.getMessage()).contains(messagePart);
                });
    }
}
