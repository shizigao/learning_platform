package com.learningplatform.auth.security;

import java.time.Instant;

public record JwtTokenClaims(
        Long userId,
        String username,
        Instant issuedAt,
        Instant expiresAt
) {
}
