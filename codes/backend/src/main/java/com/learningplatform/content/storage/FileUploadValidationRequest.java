package com.learningplatform.content.storage;

import com.learningplatform.content.domain.ContentFileRole;

public record FileUploadValidationRequest(
        ContentFileRole fileRole,
        String originalFilename,
        String mimeType,
        long sizeBytes,
        int existingFileCount,
        int existingRoleFileCount,
        long resourceOwnerId,
        long requesterUserId,
        boolean requesterAdmin
) {
}
