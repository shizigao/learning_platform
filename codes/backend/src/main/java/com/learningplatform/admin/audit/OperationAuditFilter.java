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
public class OperationAuditFilter extends OncePerRequestFilter {
    public static final String LOGIN_OPERATOR_ID =
            OperationAuditFilter.class.getName() + ".loginOperatorId";
    public static final String LOGIN_OPERATOR_NAME =
            OperationAuditFilter.class.getName() + ".loginOperatorName";
    private static final Logger LOGGER =
            LoggerFactory.getLogger(OperationAuditFilter.class);
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

    private final OperationLogService operationLogService;

    public OperationAuditFilter(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return findRule(request).match() == null;
    }

    @Override
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

    private String requestId(HttpServletResponse response) {
        String value = MDC.get("traceId");
        return value == null ? response.getHeader(TraceIdFilter.HEADER_NAME) : value;
    }

    private String path(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        return context.isEmpty() ? uri : uri.substring(context.length());
    }

    private int durationMillis(long startedAt) {
        long elapsed = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
        return (int) Math.min(Math.max(elapsed, 0), MAX_DURATION_MS);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength);
    }

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

    private record AuditRule(
            String method,
            Pattern pathPattern,
            String module,
            String action,
            String targetType
    ) {
    }

    private record RuleMatch(AuditRule rule, Matcher match) {
    }
}
