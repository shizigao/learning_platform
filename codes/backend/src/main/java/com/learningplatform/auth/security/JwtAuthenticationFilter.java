/* 文件职责：在 Servlet 过滤链中处理JWT认证过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
/**
 * 在 Servlet 过滤链中处理JWT认证过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    /** 定义 BEARER_PREFIX 常量，统一该组件使用的固定规则或默认值。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 委托令牌执行对应领域规则。 */
    private final JwtTokenService tokenService;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 委托角色执行对应领域规则。 */
    private final RoleService roleService;
    /** 缓存认证所需的用户状态与角色快照，Redis 故障时自动回源数据库。 */
    private final AuthSnapshotCache authSnapshotCache;
    /** 保存认证EntryPoint，供该类型的业务逻辑读取或更新。 */
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public JwtAuthenticationFilter(
            JwtTokenService tokenService,
            UserService userService,
            RoleService roleService,
            AuthSnapshotCache authSnapshotCache,
            ApiAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.roleService = roleService;
        this.authSnapshotCache = authSnapshotCache;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    /** 执行 shouldNotFilter 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/health")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/error")
                || path.startsWith("/actuator/health/");
    }

    @Override
    /** 执行 doFilterInternal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || authorization.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)
                || authorization.length() == BEARER_PREFIX.length()) {
            reject(request, response, new BadCredentialsException("Bearer Token格式不正确"));
            return;
        }

        try {
            authenticate(request, authorization.substring(BEARER_PREFIX.length()));
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            reject(request, response, new BadCredentialsException("Token无效或已过期", exception));
        }
    }

    /** 执行 authenticate 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void authenticate(HttpServletRequest request, String token) {
        JwtTokenClaims claims = tokenService.parse(token);
        AuthSnapshotCache.Snapshot snapshot = authSnapshotCache.getOrLoad(
                claims.userId(),
                () -> {
                    User user = userService.findById(claims.userId())
                            .orElseThrow(() -> new BadCredentialsException("Token对应用户不存在"));
                    return new AuthSnapshotCache.Snapshot(
                            user.getId(),
                            user.getUsername(),
                            user.getStatus(),
                            roleService.findRoleCodesByUserId(user.getId())
                    );
                }
        );
        if (!snapshot.username().equals(claims.username())) {
            throw new BadCredentialsException("Token用户信息不一致");
        }
        if (snapshot.status() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("账号当前不可用");
        }

        Set<RoleCode> roles = snapshot.roles();
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(snapshot.userId(), snapshot.username(), roles);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 执行驳回状态流转，仅允许从合法前置状态进入目标状态。 */
    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        authenticationEntryPoint.commence(request, response, exception);
    }
}
