/* 文件职责：表示Api认证EntryPoint领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.api.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
/**
 * 表示Api认证EntryPoint领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ApiAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    /** 执行 commence 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(ErrorCode.UNAUTHORIZED));
    }
}
