package com.learningplatform.content.storage;

import com.learningplatform.content.domain.ContentFileRole;

import java.io.InputStream;

public record StorageUploadRequest(
        ContentFileRole fileRole,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        int existingFileCount,
        int existingRoleFileCount,
        long resourceOwnerId,
        long requesterUserId,
        boolean requesterAdmin,
        InputStream inputStream
) {
    FileUploadValidationRequest toValidationRequest() {
        return new FileUploadValidationRequest(
                fileRole,
                originalFilename,
                mimeType,
                sizeBytes,
                existingFileCount,
                existingRoleFileCount,
                resourceOwnerId,
                requesterUserId,
                requesterAdmin
        );
    }
}
