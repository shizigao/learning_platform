/* 文件职责：提供权益相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.order.dto.EntitlementBalancesResponse;
import com.learningplatform.order.dto.EntitlementResponse;
import com.learningplatform.order.service.EntitlementService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entitlements")
/**
 * 提供权益相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class EntitlementController {
    /** 委托权益执行对应领域规则。 */
    private final EntitlementService entitlementService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public EntitlementController(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<EntitlementResponse>> list(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(entitlementService.list(userId));
    }

    @GetMapping("/balances")
    /** 处理 GET /balances 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<EntitlementBalancesResponse> balances(Authentication authentication) {
        Long userId = AuthenticationPrincipalResolver.require(authentication).userId();
        return ApiResponse.success(entitlementService.balances(userId));
    }
}
