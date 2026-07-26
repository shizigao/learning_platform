package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassRole;
import com.learningplatform.classroom.domain.ClassStatus;

import java.time.LocalDateTime;

public record ClassSummaryResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        ClassStatus status,
        ClassRole currentRole,
        long memberCount,
        String inviteCode,
        Boolean inviteEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
