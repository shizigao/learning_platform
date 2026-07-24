package com.learningplatform.auth.security;

import com.learningplatform.common.config.JwtProperties;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Date;
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

    @Test
    void rejectsFutureIssuedAtAndInvalidSubjectClaims() {
        JwtTokenService tokenService =
                new JwtTokenService(new JwtProperties(
                        SECRET,
                        Duration.ofMinutes(30)
                ));
        Instant future = Instant.now().plusSeconds(300);
        String futureToken = token(
                "10",
                future,
                future.plusSeconds(1800)
        );
        String invalidSubjectToken = token(
                "0",
                Instant.now(),
                Instant.now().plusSeconds(1800)
        );

        assertThatThrownBy(() -> tokenService.parse(futureToken))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokenService.parse(invalidSubjectToken))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private String token(String subject, Instant issuedAt, Instant expiresAt) {
        return Jwts.builder()
                .subject(subject)
                .claim("username", "alice")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(Keys.hmacShaKeyFor(
                        SECRET.getBytes(StandardCharsets.UTF_8)
                ))
                .compact();
    }

    private User user() {
        User user = new User();
        user.setId(10L);
        user.setUsername("alice");
        return user;
    }
}
