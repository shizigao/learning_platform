/* 文件职责：实现JWT令牌业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现JWT令牌业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class JwtTokenService {
    /** 定义 MIN_SECRET_BYTES 常量，统一该组件使用的固定规则或默认值。 */
    private static final int MIN_SECRET_BYTES = 32;

    /** 保存配置属性，供该类型的业务逻辑读取或更新。 */
    private final JwtProperties properties;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    /** 判断是否满足sue条件，不修改持久化状态。 */
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

    /** 转换或规范化数据，不引入额外持久化副作用。 */
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

    /** 执行 expiresInSeconds 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public long expiresInSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    /** 执行 signingKey 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private SecretKey signingKey() {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JWT密钥配置不安全");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
