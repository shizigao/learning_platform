package com.learningplatform.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "订单至少包含一个商品")
        @Size(max = 20, message = "单个订单最多包含20种商品")
        List<@Valid OrderCreateItemRequest> items,

        @Size(max = 500, message = "订单备注不能超过500个字符")
        String remark
) {
    public OrderCreateRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
