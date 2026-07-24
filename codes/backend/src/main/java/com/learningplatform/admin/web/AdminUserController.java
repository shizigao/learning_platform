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
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminUserResponse>> list(
            @Valid @ModelAttribute AdminUserListQuery query
    ) {
        return ApiResponse.success(adminUserService.list(query));
    }

    @GetMapping("/{userId}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long userId) {
        return ApiResponse.success(adminUserService.detail(userId));
    }

    @PutMapping("/{userId}/status")
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

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }
}
