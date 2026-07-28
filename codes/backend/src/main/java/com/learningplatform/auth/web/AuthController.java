/* 文件职责：提供认证相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.web;

import com.learningplatform.auth.dto.LoginRequest;
import com.learningplatform.auth.dto.LoginResponse;
import com.learningplatform.auth.dto.RegisterRequest;
import com.learningplatform.auth.dto.UpdateProfileRequest;
import com.learningplatform.auth.dto.UserProfileResponse;
import com.learningplatform.auth.service.AuthService;
import com.learningplatform.admin.audit.OperationAuditFilter;
import com.learningplatform.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
/**
 * 提供认证相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AuthController {
    /** 委托认证执行对应领域规则。 */
    private final AuthService authService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    /** 处理 POST /register 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    /** 处理 POST /login 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        httpRequest.setAttribute(
                OperationAuditFilter.LOGIN_OPERATOR_NAME,
                request.username().trim().toLowerCase(java.util.Locale.ROOT)
        );
        LoginResponse response =
                // 点击login
                authService.login(request, httpRequest.getRemoteAddr());
        httpRequest.setAttribute(
                OperationAuditFilter.LOGIN_OPERATOR_ID,
                response.user().id()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    /** 处理 POST /logout 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @GetMapping("/me")
    /** 处理 GET /me 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<UserProfileResponse> currentUser(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication));
    }

    @PutMapping("/me")
    /** 处理 PUT /me 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<UserProfileResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(authService.updateCurrentUser(authentication, request));
    }
}
