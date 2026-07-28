/* 文件职责：提供健康检查相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.web;

import com.learningplatform.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
/**
 * 提供健康检查相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class HealthController {

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of(
                "status", "UP",
                "application", "learning-platform-backend"
        ));
    }
}

