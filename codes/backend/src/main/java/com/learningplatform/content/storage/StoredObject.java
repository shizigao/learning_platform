package com.learningplatform.content.storage;

public record StoredObject(
        String bucketName,
        String objectName,
        String originalFilename,
        String mimeType,
        String extension,
        long sizeBytes
) {
}
