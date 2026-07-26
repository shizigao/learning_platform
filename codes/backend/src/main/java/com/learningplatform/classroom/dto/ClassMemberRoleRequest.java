package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassRole;
import jakarta.validation.constraints.NotNull;

public record ClassMemberRoleRequest(
        @NotNull(message = "班级角色不能为空")
        ClassRole role
) {
}
