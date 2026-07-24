package com.learningplatform.auth.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.JwtProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Component
public class JwtTokenService {
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    public String issue(User user, Set<RoleCode> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("roles", roles.stream().map(RoleCode::name).sorted().toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey())
                .compact();
    }

    public JwtTokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        if (username == null || username.isBlank()
                || claims.getIssuedAt() == null || claims.getExpiration() == null) {
            throw new IllegalArgumentException("JWT required claims are missing");
        }
        Instant issuedAt = claims.getIssuedAt().toInstant();
        Instant expiresAt = claims.getExpiration().toInstant();
        if (userId <= 0
                || !expiresAt.isAfter(issuedAt)
                || issuedAt.isAfter(Instant.now().plusSeconds(60))) {
            throw new IllegalArgumentException("JWT claims are invalid");
        }
        return new JwtTokenClaims(
                userId,
                username,
                issuedAt,
                expiresAt
        );
    }

    public long expiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    private SecretKey signingKey() {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT密钥配置不安全");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
