package com.learningplatform.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
}
