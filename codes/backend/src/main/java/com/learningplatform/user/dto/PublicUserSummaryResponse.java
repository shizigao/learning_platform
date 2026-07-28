/* 文件职责：定义Public用户总结响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.dto;

import com.learningplatform.user.domain.User;

/**
 * 定义Public用户总结响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record PublicUserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String bio
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static PublicUserSummaryResponse from(User user, String avatarUrl) {
        return new PublicUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                avatarUrl,
                user.getBio()
        );
    }
}
