/* 文件职责：定义Login请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 定义Login请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名不能超过64个字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码不能超过64个字符")
        String password
) {
    @Override
    /** 返回适合日志记录的文本表示；敏感 DTO 必须对密码、令牌或证件信息脱敏。 */
    public String toString() {
        return "LoginRequest[username=" + username + ", password=[REDACTED]]";
    }
}
