/* 文件职责：在 Servlet 过滤链中处理链路追踪Id过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
/**
 * 在 Servlet 过滤链中处理链路追踪Id过滤器，并在请求进入 Controller 前建立安全或上下文约束。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class TraceIdFilter extends OncePerRequestFilter {
    /** 定义 HEADER_NAME 常量，统一该组件使用的固定规则或默认值。 */
    public static final String HEADER_NAME = "X-Request-Id";
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    /** 执行 doFilterInternal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestedTraceId = request.getHeader(HEADER_NAME);
        String traceId = requestedTraceId != null && SAFE_TRACE_ID.matcher(requestedTraceId).matches()
                ? requestedTraceId
                : UUID.randomUUID().toString().replace("-", "");

        MDC.put("traceId", traceId);
        response.setHeader(HEADER_NAME, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}

