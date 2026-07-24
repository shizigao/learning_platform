package com.learningplatform.admin.dto;

import com.learningplatform.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(
        @NotNull(message = "用户状态不能为空")
        UserStatus status
) {
}
