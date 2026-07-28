/* 文件职责：以不可变记录表示JWT令牌Claims数据，并作为模块内部或接口层的数据契约。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import java.time.Instant;

/**
 * 以不可变记录表示JWT令牌Claims数据，并作为模块内部或接口层的数据契约。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public record JwtTokenClaims(
        Long userId,
        String username,
        Instant issuedAt,
        Instant expiresAt
) {
}
