/* 文件职责：定义Public用户资料响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.dto;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 定义Public用户资料响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record PublicUserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio,
        Set<RoleCode> roles,
        LocalDateTime createdAt,
        UserPublicationStatsResponse statistics
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static PublicUserProfileResponse from(
            User user,
            Set<RoleCode> roles,
            String avatarUrl,
            UserPublicationStatsResponse statistics
    ) {
        return new PublicUserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                avatarUrl,
                user.getBio(),
                Set.copyOf(roles),
                user.getCreatedAt(),
                statistics
        );
    }
}
