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
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService tokenService;
    private final UserService userService;
    private final RoleService roleService;
    private final ApiAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtTokenService tokenService,
            UserService userService,
            RoleService roleService,
            ApiAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.roleService = roleService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/api/health")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/error")
                || path.startsWith("/actuator/health/");
    }

    @Override
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

    private void authenticate(HttpServletRequest request, String token) {
        JwtTokenClaims claims = tokenService.parse(token);
        User user = userService.findById(claims.userId())
                .orElseThrow(() -> new BadCredentialsException("Token对应用户不存在"));
        if (!user.getUsername().equals(claims.username())) {
            throw new BadCredentialsException("Token用户信息不一致");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("账号当前不可用");
        }

        Set<RoleCode> roles = roleService.findRoleCodesByUserId(user.getId());
        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(user.getId(), user.getUsername(), roles);
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

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        authenticationEntryPoint.commence(request, response, exception);
    }
}
