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
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        httpRequest.setAttribute(
                OperationAuditFilter.LOGIN_OPERATOR_NAME,
                request.username().trim().toLowerCase(java.util.Locale.ROOT)
        );
        LoginResponse response =
                authService.login(request, httpRequest.getRemoteAddr());
        httpRequest.setAttribute(
                OperationAuditFilter.LOGIN_OPERATOR_ID,
                response.user().id()
        );
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> currentUser(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ApiResponse.success(authService.updateCurrentUser(authentication, request));
    }
}
