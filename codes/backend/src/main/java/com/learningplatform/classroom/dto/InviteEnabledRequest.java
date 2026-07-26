package com.learningplatform.classroom.dto;

import jakarta.validation.constraints.NotNull;

public record InviteEnabledRequest(
        @NotNull(message = "必须明确邀请码是否启用")
        Boolean enabled
) {
}
