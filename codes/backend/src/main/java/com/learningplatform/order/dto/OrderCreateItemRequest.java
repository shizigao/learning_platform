package com.learningplatform.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderCreateItemRequest(
        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID必须大于0")
        Long productId,

        @Min(value = 1, message = "购买数量必须大于0")
        @Max(value = 99, message = "单项购买数量不能超过99")
        int quantity
) {
}
