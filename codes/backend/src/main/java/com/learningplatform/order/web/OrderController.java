/* 文件职责：提供订单相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.order.dto.MockPaymentResponse;
import com.learningplatform.order.dto.OrderCreateRequest;
import com.learningplatform.order.dto.OrderListQuery;
import com.learningplatform.order.dto.OrderResponse;
import com.learningplatform.order.service.MockPaymentService;
import com.learningplatform.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/orders")
/**
 * 提供订单相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class OrderController {
    /** 委托订单执行对应领域规则。 */
    private final OrderService orderService;
    /** 委托支付执行对应领域规则。 */
    private final MockPaymentService paymentService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OrderController(
            OrderService orderService,
            MockPaymentService paymentService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<OrderResponse> create(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return ApiResponse.success(orderService.create(userId(authentication), request));
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<OrderResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute OrderListQuery query
    ) {
        return ApiResponse.success(orderService.list(userId(authentication), query));
    }

    @GetMapping("/{orderId}")
    /** 处理 GET /{orderId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<OrderResponse> detail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.detail(orderId, userId(authentication)));
    }

    @PostMapping("/{orderId}/cancel")
    /** 处理 POST /{orderId}/cancel 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<OrderResponse> cancel(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.cancel(orderId, userId(authentication)));
    }

    @PostMapping("/{orderId}/mock-pay")
    /** 处理 POST /{orderId}/mock-pay 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<MockPaymentResponse> mockPay(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(paymentService.pay(orderId, userId(authentication)));
    }

    /** 处理 POST /{orderId}/mock-pay 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
