package com.learningplatform.auth.security;

import com.learningplatform.common.config.JwtProperties;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTests {
    private static final String SECRET = "test-only-secret-with-at-least-thirty-two-bytes";

    @Test
    void issuesAndParsesSignedToken() {
        JwtTokenService tokenService =
                new JwtTokenService(new JwtProperties(SECRET, Duration.ofMinutes(30)));
        User user = user();

        JwtTokenClaims claims = tokenService.parse(
                tokenService.issue(user, Set.of(RoleCode.USER, RoleCode.PUBLISHER))
        );

        assertThat(claims.userId()).isEqualTo(10L);
        assertThat(claims.username()).isEqualTo("alice");
        assertThat(claims.expiresAt()).isAfter(claims.issuedAt());
    }

    @Test
    void rejectsExpiredAndTamperedTokens() {
        User user = user();
        JwtTokenService expiredTokenService =
                new JwtTokenService(new JwtProperties(SECRET, Duration.ofSeconds(-1)));
        String expiredToken = expiredTokenService.issue(user, Set.of(RoleCode.USER));
        assertThatThrownBy(() -> expiredTokenService.parse(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);

        JwtTokenService tokenService =
                new JwtTokenService(new JwtProperties(SECRET, Duration.ofMinutes(30)));
        String validToken = tokenService.issue(user, Set.of(RoleCode.USER));
        String tamperedToken = validToken.substring(0, validToken.length() - 2) + "xx";
        assertThatThrownBy(() -> tokenService.parse(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        return user;
    }
}
