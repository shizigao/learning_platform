/* 文件职责：实现认证业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
import com.learningplatform.user.service.UserAvatarService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Service
/**
 * 实现认证业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AuthService {
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 委托角色执行对应领域规则。 */
    private final RoleService roleService;
    /** 保存passwordEncoder，供该类型的业务逻辑读取或更新。 */
    private final PasswordEncoder passwordEncoder;
    /** 委托令牌执行对应领域规则。 */
    private final JwtTokenService tokenService;
    /** 委托loginProtection执行对应领域规则。 */
    private final LoginProtectionService loginProtectionService;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AuthService(
            UserService userService,
            RoleService roleService,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService,
            LoginProtectionService loginProtectionService,
            UserAvatarService avatarService
    ) {
        this.userService = userService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginProtectionService = loginProtectionService;
        this.avatarService = avatarService;
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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
    /** 执行 login 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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
        // 点击recordSuccess
        loginProtectionService.recordSuccess(username);
        userService.recordLogin(user.getId(), loginIp);
        return new LoginResponse(
                tokenService.issue(user, roles),
                "Bearer",
                tokenService.expiresInSeconds(),
                UserProfileResponse.from(user, roles, avatarService.avatarUrl(user))
        );
    }

    /** 执行 currentUser 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public UserProfileResponse currentUser(Authentication authentication) {
        return profile(resolveAuthenticatedUser(authentication));
    }

    @Transactional
    /** 更新Current用户，通过返回值或版本条件识别并发状态变化。 */
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

    /** 执行 resolveAuthenticatedUser 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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

    /** 执行 profile 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private UserProfileResponse profile(User user) {
        return UserProfileResponse.from(
                user,
                roleService.findRoleCodesByUserId(user.getId()),
                avatarService.avatarUrl(user)
        );
    }

    /** 校验Unique资料及相关业务前置条件，不满足时抛出明确业务异常。 */
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

    /** 执行 invalidCredentials 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalidCredentials() {
        return new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
    }

    /** 转换或规范化Nullable数据，不引入额外持久化副作用。 */
    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
