/* 文件职责：定义Login响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.dto;

/**
 * 定义Login响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserProfileResponse user
) {
    @Override
    /** 返回适合日志记录的文本表示；敏感 DTO 必须对密码、令牌或证件信息脱敏。 */
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], tokenType=" + tokenType
                + ", expiresIn=" + expiresIn + ", user=" + user + "]";
    }
}
