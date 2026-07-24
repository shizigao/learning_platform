package com.learningplatform.order.web;

import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.order.dto.AdminOrderListQuery;
import com.learningplatform.order.dto.OrderResponse;
import com.learningplatform.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResult<OrderResponse>> list(
            @Valid @ModelAttribute AdminOrderListQuery query
    ) {
        return ApiResponse.success(orderService.listForAdmin(query));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long orderId) {
        return ApiResponse.success(orderService.detailForAdmin(orderId));
    }
}
