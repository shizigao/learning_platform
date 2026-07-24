package com.learningplatform.admin.dto;

import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminUserRolesRequest(
        @NotEmpty(message = "用户至少需要保留一个角色")
        @Size(max = 3, message = "角色数量不能超过3个")
        Set<@NotNull(message = "角色不能为空") RoleCode> roles
) {
}
