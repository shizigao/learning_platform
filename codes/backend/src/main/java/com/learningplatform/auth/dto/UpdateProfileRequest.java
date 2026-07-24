package com.learningplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
