/* 文件职责：定义更新资料请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 定义更新资料请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record UpdateProfileRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称不能超过64个字符")
        String nickname,

        @Size(max = 512, message = "头像地址不能超过512个字符")
        String avatarUrl,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过128个字符")
        String email,

        @Pattern(regexp = "^$|^\\+?[0-9]{6,20}$", message = "手机号格式不正确")
        String phone,

        @Pattern(regexp = "^$|^(UNKNOWN|MALE|FEMALE)$", message = "性别代码不正确")
        String gender,

        @Size(max = 500, message = "个人简介不能超过500个字符")
        String bio
) {
}
