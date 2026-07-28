/* 文件职责：在 Servlet 过滤链中处理操作审核过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 * 所属模块：平台治理与管理员操作；所在分层：审计基础设施层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.audit;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.common.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
/**
 * 在 Servlet 过滤链中处理操作审核过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 *
 * <p>职责边界：遵守 平台治理与管理员操作 模块的职责边界。</p>
 */
public class OperationAuditFilter extends OncePerRequestFilter {
    /** 定义 LOGIN_OPERATOR_ID 常量，统一该组件使用的固定规则或默认值。 */
    public static final String LOGIN_OPERATOR_ID =
            OperationAuditFilter.class.getName() + ".loginOperatorId";
    /** 定义 LOGIN_OPERATOR_NAME 常量，统一该组件使用的固定规则或默认值。 */
    public static final String LOGIN_OPERATOR_NAME =
            OperationAuditFilter.class.getName() + ".loginOperatorName";
    /** 记录关键状态变化和异常上下文，不输出密码、密钥或敏感正文。 */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OperationAuditFilter.class);
    /** 定义 MAX_DURATION_MS 常量，统一该组件使用的固定规则或默认值。 */
    private static final int MAX_DURATION_MS = Integer.MAX_VALUE;
    private static final List<AuditRule> RULES = List.of(
            rule("POST", "^/api/auth/login$", "AUTH", "LOGIN", "USER"),
            rule("PUT", "^/api/admin/users/(?<target>\\d+)/roles$",
                    "USER", "CHANGE_ROLES", "USER"),
            rule("PUT", "^/api/admin/users/(?<target>\\d+)/status$",
                    "USER", "CHANGE_STATUS", "USER"),
            rule("POST", "^/api/admin/contents/(?<target>\\d+)/approve$",
                    "CONTENT", "APPROVE", "CONTENT"),
            rule("POST", "^/api/admin/contents/(?<target>\\d+)/reject$",
                    "CONTENT", "REJECT", "CONTENT"),
            rule("POST", "^/api/admin/contents/(?<target>\\d+)/offline$",
                    "CONTENT", "OFFLINE", "CONTENT"),
            rule("POST", "^/api/admin/contents/(?<target>\\d+)/publish$",
                    "CONTENT", "PUBLISH", "CONTENT"),
            rule("POST", "^/api/orders$", "ORDER", "CREATE", "ORDER"),
            rule("POST", "^/api/orders/(?<target>\\d+)/cancel$",
                    "ORDER", "CANCEL", "ORDER"),
            rule("POST", "^/api/orders/(?<target>\\d+)/mock-pay$",
                    "ORDER", "MOCK_PAY_AND_GRANT_ENTITLEMENT", "ORDER"),
            rule("POST", "^/api/publisher/exams/(?<target>\\d+)/publish$",
                    "EXAM", "PUBLISH", "EXAM"),
            rule("PUT",
                    "^/api/publisher/exams/\\d+/grading/attempts/\\d+"
                            + "/answers/(?<target>\\d+)$",
                    "GRADING", "GRADE_ANSWER", "EXAM_ANSWER"),
            rule("POST",
                    "^/api/publisher/exams/\\d+/grading/attempts/"
                            + "(?<target>\\d+)/complete$",
                    "GRADING", "COMPLETE_REVIEW", "EXAM_ATTEMPT")
    );

    /** 委托操作日志执行对应领域规则。 */
    private final OperationLogService operationLogService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OperationAuditFilter(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    /** 执行 shouldNotFilter 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return findRule(request).match() == null;
    }

    @Override
    /** 执行 doFilterInternal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RuleMatch ruleMatch = findRule(request);
        if (ruleMatch.match() == null) {
            filterChain.doFilter(request, response);
            return;
        }
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            recordSafely(request, response, ruleMatch, startedAt);
        }
    }

    /** 执行 recordSafely 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void recordSafely(
            HttpServletRequest request,
            HttpServletResponse response,
            RuleMatch ruleMatch,
            long startedAt
    ) {
        try {
            OperationLog log = new OperationLog();
            applyOperator(log, request);
            log.setModule(ruleMatch.rule().module());
            log.setAction(ruleMatch.rule().action());
            log.setTargetType(ruleMatch.rule().targetType());
            log.setTargetId(targetId(ruleMatch.match(), request));
            log.setRequestMethod(request.getMethod());
            log.setRequestPath(limit(path(request), 512));
            log.setRequestId(limit(requestId(response), 64));
            log.setIpAddress(limit(request.getRemoteAddr(), 64));
            log.setUserAgent(limit(request.getHeader("User-Agent"), 1000));
            int status = response.getStatus();
            log.setResult(status < 400
                    ? OperationResult.SUCCESS
                    : OperationResult.FAILURE);
            log.setDetailJson("{\"httpStatus\":" + status + "}");
            log.setErrorMessage(status < 400 ? null : "HTTP " + status);
            log.setDurationMs(durationMillis(startedAt));
            operationLogService.record(log);
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "OPERATION_AUDIT_WRITE_FAILED traceId={} module={} action={} cause={}",
                    MDC.get("traceId"),
                    ruleMatch.rule().module(),
                    ruleMatch.rule().action(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    /** 执行 applyOperator 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void applyOperator(OperationLog log, HttpServletRequest request) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal) {
            log.setOperatorId(principal.userId());
            log.setOperatorName(limit(principal.username(), 64));
            return;
        }
        Object operatorId = request.getAttribute(LOGIN_OPERATOR_ID);
        Object operatorName = request.getAttribute(LOGIN_OPERATOR_NAME);
        if (operatorId instanceof Long value) {
            log.setOperatorId(value);
        }
        if (operatorName instanceof String value) {
            log.setOperatorName(limit(value, 64));
        }
    }

    /** 查询Rule相关数据；只返回当前调用方有权查看的结果。 */
    private RuleMatch findRule(HttpServletRequest request) {
        String path = path(request);
        for (AuditRule rule : RULES) {
            if (!rule.method().equalsIgnoreCase(request.getMethod())) {
                continue;
            }
            Matcher matcher = rule.pathPattern().matcher(path);
            if (matcher.matches()) {
                return new RuleMatch(rule, matcher);
            }
        }
        return new RuleMatch(null, null);
    }

    /** 执行 targetId 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String targetId(Matcher matcher, HttpServletRequest request) {
        if ("/api/auth/login".equals(path(request))) {
            Object operatorId = request.getAttribute(LOGIN_OPERATOR_ID);
            return operatorId == null ? null : operatorId.toString();
        }
        try {
            return matcher.group("target");
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** 执行 requestId 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String requestId(HttpServletResponse response) {
        String value = MDC.get("traceId");
        return value == null ? response.getHeader(TraceIdFilter.HEADER_NAME) : value;
    }

    /** 执行 path 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }

    /** 执行 durationMillis 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private int durationMillis(long startedAt) {
        long elapsed = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        return (int) Math.min(Math.max(elapsed, 0), MAX_DURATION_MS);
    }

    /** 执行 limit 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

    /** 执行 rule 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private static AuditRule rule(
            String method,
            String regex,
            String module,
            String action,
            String targetType
    ) {
        return new AuditRule(
                method,
                Pattern.compile(regex),
                module,
                action,
                targetType
        );
    }

    /** 执行 AuditRule 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record AuditRule(
            String method,
            Pattern pathPattern,
            String module,
            String action,
            String targetType
    ) {
    }

    /** 执行 RuleMatch 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record RuleMatch(AuditRule rule, Matcher match) {
    }
}
