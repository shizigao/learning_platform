package com.learningplatform.auth.service;

import com.learningplatform.auth.dto.LoginRequest;
import com.learningplatform.auth.dto.LoginResponse;
import com.learningplatform.auth.dto.RegisterRequest;
import com.learningplatform.auth.dto.UpdateProfileRequest;
import com.learningplatform.auth.dto.UserProfileResponse;
import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.JwtTokenService;
import com.learningplatform.auth.security.LoginProtectionService;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
public class AuthService {
    private final UserService userService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LoginProtectionService loginProtectionService;

    public AuthService(
            UserService userService,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            LoginProtectionService loginProtectionService
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginProtectionService = loginProtectionService;
    }

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());
        validateUniqueProfile(username, email, phone, null);

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setNickname(request.nickname().trim());
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        userService.create(user);
        roleService.assignRole(user.getId(), RoleCode.USER, null);

        return profile(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String loginIp) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        loginProtectionService.checkAllowed(username, loginIp);
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            loginProtectionService.recordFailure(username, loginIp);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginProtectionService.recordFailure(username, loginIp);
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号当前不可用");
        }

        Set<RoleCode> roles = roleService.findRoleCodesByUserId(user.getId());
        loginProtectionService.recordSuccess(username);
        userService.recordLogin(user.getId(), loginIp);
        return new LoginResponse(
                tokenService.issue(user, roles),
                "Bearer",
                tokenService.expiresInSeconds(),
                UserProfileResponse.from(user, roles)
        );
    }

    public UserProfileResponse currentUser(Authentication authentication) {
        return profile(resolveAuthenticatedUser(authentication));
    }

    @Transactional
    public UserProfileResponse updateCurrentUser(
            Authentication authentication,
            UpdateProfileRequest request
    ) {
        User user = resolveAuthenticatedUser(authentication);
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());
        validateUniqueProfile(user.getUsername(), email, phone, user.getId());

        user.setNickname(request.nickname().trim());
        user.setAvatarUrl(normalizeNullable(request.avatarUrl()));
        user.setEmail(email);
        user.setPhone(phone);
        user.setGender(normalizeNullable(request.gender()));
        user.setBio(normalizeNullable(request.bio()));
        userService.updateProfile(user);
        return profile(user);
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            return userService.findById(principal.userId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        }
        return userService.findByUsername(authentication.getName())
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private UserProfileResponse profile(User user) {
        return UserProfileResponse.from(user, roleService.findRoleCodesByUserId(user.getId()));
    }

    private void validateUniqueProfile(
            String username,
            String email,
            String phone,
            Long excludedUserId
    ) {
        if (excludedUserId == null && userService.usernameExists(username)) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已被使用");
        }
        if (userService.emailExists(email, excludedUserId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "邮箱已被使用");
        }
        if (userService.phoneExists(phone, excludedUserId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已被使用");
        }
    }

    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
