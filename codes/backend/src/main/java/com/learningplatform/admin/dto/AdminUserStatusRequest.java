/* 文件职责：定义管理用户状态请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：平台治理与管理员操作；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.dto;

import com.learningplatform.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

/**
 * 定义管理用户状态请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AdminUserStatusRequest(
        @NotNull(message = "用户状态不能为空")
        UserStatus status
) {
}
