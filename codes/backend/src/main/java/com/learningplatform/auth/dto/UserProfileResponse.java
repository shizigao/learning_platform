package com.learningplatform.auth.dto;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String email,
        String phone,
        String gender,
        String bio,
        UserStatus status,
        Set<RoleCode> roles,
        LocalDateTime createdAt
) {
    public static UserProfileResponse from(User user, Set<RoleCode> roles) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                user.getGender(),
                user.getBio(),
                user.getStatus(),
                roles,
                user.getCreatedAt()
        );
    }
}
