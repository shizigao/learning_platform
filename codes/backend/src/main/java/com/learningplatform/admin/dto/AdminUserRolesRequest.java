/* 文件职责：定义管理用户Roles请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：平台治理与管理员操作；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.dto;

import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * 定义管理用户Roles请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AdminUserRolesRequest(
        @NotEmpty(message = "用户至少需要保留一个角色")
        @Size(max = 3, message = "角色数量不能超过3个")
        Set<@NotNull(message = "角色不能为空") RoleCode> roles
) {
}
