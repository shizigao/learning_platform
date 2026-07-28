/* 文件职责：定义管理用户响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：平台治理与管理员操作；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.dto;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 定义管理用户响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String phone,
        UserStatus status,
        Set<RoleCode> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public AdminUserResponse {
        roles = Set.copyOf(roles);
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static AdminUserResponse from(User user, Set<RoleCode> roles) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus(),
                roles,
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
