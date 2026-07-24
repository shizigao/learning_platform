package com.learningplatform.content.storage;

import com.learningplatform.content.domain.ContentFileRole;

public record ValidatedUploadFile(
        ContentFileRole fileRole,
        String originalFilename,
        String extension,
        String mimeType,
        long sizeBytes
) {
}
