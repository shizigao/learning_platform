/* 文件职责：在 Servlet 过滤链中处理Api频率限制过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.ApiRateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
/**
 * 在 Servlet 过滤链中处理Api频率限制过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {
    /** 定义 WINDOW_MILLIS 常量，统一该组件使用的固定规则或默认值。 */
    private static final long WINDOW_MILLIS = 60_000L;
    /** 定义 CLEANUP_INTERVAL 常量，统一该组件使用的固定规则或默认值。 */
    private static final long CLEANUP_INTERVAL = 1_000L;

    /** 保存配置属性，供该类型的业务逻辑读取或更新。 */
    private final ApiRateLimitProperties properties;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;
    /** 提供可替换时间源，便于测试时间相关规则。 */
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestsSinceCleanup = new AtomicLong();

    @Autowired
    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ApiRateLimitFilter(
            ApiRateLimitProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    ApiRateLimitFilter(
            ApiRateLimitProperties properties,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    /** 执行 shouldNotFilter 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.enabled()
                || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = requestPath(request);
        return !path.startsWith("/api/")
                || "/api/health".equals(path);
    }

    @Override
    /** 执行 doFilterInternal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        LimitGroup group = group(request);
        long now = clock.millis();
        String key = group.name() + ':' + safeAddress(request.getRemoteAddr());
        RateDecision decision = counters
                .computeIfAbsent(key, ignored -> new WindowCounter())
                .acquire(now, group.limit(properties));
        cleanupOccasionally(now);
        if (!decision.allowed()) {
            response.setStatus(ErrorCode.TOO_MANY_REQUESTS
                    .getHttpStatus().value());
            applySecurityHeaders(response);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader(
                    "Retry-After",
                    Long.toString(decision.retryAfterSeconds())
            );
            objectMapper.writeValue(
                    response.getWriter(),
                    ApiResponse.failure(ErrorCode.TOO_MANY_REQUESTS)
            );
            return;
        }
        filterChain.doFilter(request, response);
    }

    /** 执行 applySecurityHeaders 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader(
                "Referrer-Policy",
                "strict-origin-when-cross-origin"
        );
        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()"
        );
        response.setHeader(
                "Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; "
                        + "base-uri 'none'; form-action 'none'"
        );
    }

    /** 执行 group 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private LimitGroup group(HttpServletRequest request) {
        String path = requestPath(request);
        if (path.equals("/api/auth/login")
                || path.equals("/api/auth/register")) {
            return LimitGroup.AUTH;
        }
        if (isUploadRequest(request, path)) {
            return LimitGroup.UPLOAD;
        }
        return LimitGroup.GENERAL;
    }

    /** 判断是否满足上传请求条件，不修改持久化状态。 */
    private boolean isUploadRequest(
            HttpServletRequest request,
            String path
    ) {
        String contentType = request.getContentType();
        return "POST".equalsIgnoreCase(request.getMethod())
                && (path.contains("/files")
                || (contentType != null
                && contentType.toLowerCase().startsWith("multipart/")));
    }

    /** 执行 requestPath 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }

    /** 执行 safeAddress 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String safeAddress(String address) {
        if (address == null || address.isBlank()) {
            return "unknown";
        }
        return address.length() <= 64
                ? address
                : address.substring(0, 64);
    }

    /** 执行 cleanupOccasionally 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void cleanupOccasionally(long now) {
        if (requestsSinceCleanup.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        counters.entrySet().removeIf(
                entry -> entry.getValue().expired(now)
        );
    }

    private enum LimitGroup {
        GENERAL,
        AUTH,
        UPLOAD;

        int limit(ApiRateLimitProperties properties) {
            return switch (this) {
                case GENERAL -> properties.generalRequestsPerMinute();
                case AUTH -> properties.authRequestsPerMinute();
                case UPLOAD -> properties.uploadRequestsPerMinute();
            };
        }
    }

    private static final class WindowCounter {
        /** 保存窗口开始，供该类型的业务逻辑读取或更新。 */
        private long windowStart;
        /** 保存数量，供该类型的业务逻辑读取或更新。 */
        private int count;

        synchronized RateDecision acquire(long now, int limit) {
            if (windowStart == 0 || now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                count = 0;
            }
            if (count >= limit) {
                long remaining = WINDOW_MILLIS - (now - windowStart);
                long retryAfter = Math.max(
                        1,
                        (remaining + 999L) / 1_000L
                );
                return new RateDecision(false, retryAfter);
            }
            count++;
            return new RateDecision(true, 0);
        }

        synchronized boolean expired(long now) {
            return windowStart > 0
                    && now - windowStart >= WINDOW_MILLIS * 2;
        }
    }

    /** 执行 RateDecision 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record RateDecision(boolean allowed, long retryAfterSeconds) {
    }
}
