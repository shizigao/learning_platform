package com.learningplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_]{4,32}$", message = "用户名须为4至32位字母、数字或下划线")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 64, message = "密码长度须为8至64位")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 64, message = "昵称不能超过64个字符")
        String nickname,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过128个字符")
        String email,

        @Pattern(regexp = "^$|^\\+?[0-9]{6,20}$", message = "手机号格式不正确")
        String phone
) {
    @Override
    public String toString() {
        return "RegisterRequest[username=" + username
                + ", password=[REDACTED], nickname=" + nickname
                + ", email=" + email + ", phone=" + phone + "]";
    }
}
