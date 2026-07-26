package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassAnnouncement;

import java.time.LocalDateTime;

public record AnnouncementResponse(
        Long id,
        Long classId,
        Long authorId,
        String authorName,
        String authorAvatarUrl,
        String title,
        String body,
        Boolean pinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnnouncementResponse from(
            ClassAnnouncement announcement,
            String authorAvatarUrl
    ) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getClassId(),
                announcement.getAuthorId(),
                announcement.getAuthorName(),
                authorAvatarUrl,
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getPinned(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
