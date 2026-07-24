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
public class OrderController {
    private final OrderService orderService;
    private final MockPaymentService paymentService;

    public OrderController(
            OrderService orderService,
            MockPaymentService paymentService
    ) {
        this.orderService = orderService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<OrderResponse> create(
            Authentication authentication,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        return ApiResponse.success(orderService.create(userId(authentication), request));
    }

    @GetMapping
    public ApiResponse<PageResult<OrderResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute OrderListQuery query
    ) {
        return ApiResponse.success(orderService.list(userId(authentication), query));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> detail(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.detail(orderId, userId(authentication)));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderResponse> cancel(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(orderService.cancel(orderId, userId(authentication)));
    }

    @PostMapping("/{orderId}/mock-pay")
    public ApiResponse<MockPaymentResponse> mockPay(
            Authentication authentication,
            @PathVariable Long orderId
    ) {
        return ApiResponse.success(paymentService.pay(orderId, userId(authentication)));
    }

    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
