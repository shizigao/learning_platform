package com.learningplatform.content.storage;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.MinioProperties;
import com.learningplatform.common.config.UploadProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentFileRole;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MinioStorageServiceTests {
    private MinioClient minioClient;
    private MinioStorageService storageService;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        UploadProperties uploadProperties = new UploadProperties(
                DataSize.ofMegabytes(200),
                20,
                Duration.ofMinutes(10)
        );
        storageService = new MinioStorageService(
                minioClient,
                new MinioProperties(
                        "http://localhost:9000",
                        "test-access-key",
                        "test-secret-key",
                        "learning-platform-test"
                ),
                uploadProperties,
                new FileUploadValidator(uploadProperties),
                new StorageObjectKeyFactory()
        );
    }

    @Test
    void uploadsValidatedFileToPrivateBucket() throws Exception {
        StoredObject stored = storageService.upload(new StorageUploadRequest(
                ContentFileRole.COVER,
                "cover.png",
                "image/png",
                4,
                0,
                0,
                7,
                7,
                false,
                new ByteArrayInputStream(new byte[]{1, 2, 3, 4})
        ));

        assertThat(stored.bucketName()).isEqualTo("learning-platform-test");
        assertThat(stored.objectName()).matches("content/7/[0-9]{4}/[0-9]{2}/[0-9a-f-]{36}\\.png");
        assertThat(stored.originalFilename()).isEqualTo("cover.png");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void createsShortLivedSignedUrls() throws Exception {
        String objectName = validObjectName(7);
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/signed");

        String url = storageService.createSignedGetUrl(
                objectName,
                7,
                false,
                Duration.ofMinutes(5)
        );

        assertThat(url).isEqualTo("http://localhost:9000/signed");
    }

    @Test
    void rejectsSignedUrlTtlBeyondConfiguredMaximum() throws Exception {
        assertThatThrownBy(() -> storageService.createSignedGetUrl(
                validObjectName(7),
                7,
                false,
                Duration.ofMinutes(11)
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));

        verify(minioClient, never()).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void blocksCrossOwnerDownloadBeforeCallingMinio() throws Exception {
        assertThatThrownBy(() -> storageService.download(validObjectName(7), 8, false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(minioClient, never()).getObject(any(GetObjectArgs.class));
    }

    @Test
    void downloadsOwnedObjectAndLetsAdminDeleteIt() throws Exception {
        String objectName = validObjectName(7);
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        assertThat(storageService.download(objectName, 7, false)).isSameAs(response);
        storageService.delete(objectName, 8, true);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    private String validObjectName(long ownerId) {
        return "content/%d/2026/07/123e4567-e89b-42d3-a456-426614174000.pdf".formatted(ownerId);
    }
}
