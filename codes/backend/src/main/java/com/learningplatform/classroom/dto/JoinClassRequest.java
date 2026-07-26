package com.learningplatform.classroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinClassRequest(
        @NotBlank(message = "班级邀请码不能为空")
        @Size(max = 32, message = "班级邀请码格式错误")
        String inviteCode
) {
}
