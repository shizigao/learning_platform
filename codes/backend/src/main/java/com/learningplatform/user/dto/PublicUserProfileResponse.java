package com.learningplatform.user.dto;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;

import java.time.LocalDateTime;
import java.util.Set;

public record PublicUserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        Set<RoleCode> roles,
        LocalDateTime createdAt,
        UserPublicationStatsResponse statistics
) {
    public static PublicUserProfileResponse from(
            User user,
            Set<RoleCode> roles,
            String avatarUrl,
            UserPublicationStatsResponse statistics
    ) {
        return new PublicUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                avatarUrl,
                user.getBio(),
                Set.copyOf(roles),
                user.getCreatedAt(),
                statistics
        );
    }
}
