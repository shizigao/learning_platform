package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentFile;
import com.learningplatform.content.domain.ContentFileRole;

public record ContentFileResponse(
        Long id,
        ContentFileRole fileRole,
        String originalName,
        String mimeType,
        String extension,
        Long sizeBytes,
        Integer sortOrder,
        Integer durationSeconds
) {
    public static ContentFileResponse from(ContentFile file) {
        return new ContentFileResponse(
                file.getId(),
                file.getFileRole(),
                file.getOriginalName(),
                file.getMimeType(),
                file.getExtension(),
                file.getSizeBytes(),
                file.getSortOrder(),
                file.getDurationSeconds()
        );
    }
}
