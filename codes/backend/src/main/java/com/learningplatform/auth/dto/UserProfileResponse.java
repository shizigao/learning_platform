/* 文件职责：定义用户资料响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.dto;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 定义用户资料响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static UserProfileResponse from(User user, Set<RoleCode> roles) {
        return from(user, roles, user.getAvatarUrl());
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static UserProfileResponse from(
            User user,
            Set<RoleCode> roles,
            String resolvedAvatarUrl
    ) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                resolvedAvatarUrl,
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
