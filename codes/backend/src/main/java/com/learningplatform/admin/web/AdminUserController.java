/* 文件职责：提供管理用户相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：平台治理与管理员操作；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.web;

import com.learningplatform.admin.dto.AdminUserListQuery;
import com.learningplatform.admin.dto.AdminUserResponse;
import com.learningplatform.admin.dto.AdminUserRolesRequest;
import com.learningplatform.admin.dto.AdminUserStatusRequest;
import com.learningplatform.admin.service.AdminUserService;
import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/users")
/**
 * 提供管理用户相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AdminUserController {
    /** 委托管理用户执行对应领域规则。 */
    private final AdminUserService adminUserService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<AdminUserResponse>> list(
            @Valid @ModelAttribute AdminUserListQuery query
    ) {
        return ApiResponse.success(adminUserService.list(query));
    }

    @GetMapping("/{userId}")
    /** 处理 GET /{userId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long userId) {
        return ApiResponse.success(adminUserService.detail(userId));
    }

    @PutMapping("/{userId}/status")
    /** 处理 PUT /{userId}/status 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AdminUserResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserStatusRequest request
    ) {
        return ApiResponse.success(adminUserService.updateStatus(
                principal(authentication).userId(),
                userId,
                request.status()
        ));
    }

    @PutMapping("/{userId}/roles")
    /** 处理 PUT /{userId}/roles 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AdminUserResponse> replaceRoles(
            Authentication authentication,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRolesRequest request
    ) {
        return ApiResponse.success(adminUserService.replaceRoles(
                principal(authentication).userId(),
                userId,
                request.roles()
        ));
    }

    /** 处理 PUT /{userId}/roles 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }
}
