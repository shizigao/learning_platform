package com.learningplatform.user.dto;

import com.learningplatform.user.domain.User;

public record PublicUserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio
) {
    public static PublicUserSummaryResponse from(User user, String avatarUrl) {
        return new PublicUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                avatarUrl,
                user.getBio()
        );
    }
}
