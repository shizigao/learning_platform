package com.learningplatform.order.dto;

import com.learningplatform.order.domain.ProductStatus;
import com.learningplatform.order.domain.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductWriteRequest(
        @NotBlank(message = "商品编码不能为空")
        @Size(max = 64, message = "商品编码不能超过64个字符")
        @Pattern(
                regexp = "[A-Za-z0-9][A-Za-z0-9_-]*",
                message = "商品编码只能包含字母、数字、下划线和连字符"
        )
        String productCode,

        @NotNull(message = "商品类型不能为空")
        ProductType productType,

        @NotBlank(message = "商品名称不能为空")
        @Size(max = 200, message = "商品名称不能超过200个字符")
        String name,

        @Size(max = 1000, message = "商品描述不能超过1000个字符")
        String description,

        @Min(value = 1, message = "资料ID必须大于0")
        Long resourceId,

        @Min(value = 1, message = "次数包数量必须大于0")
        Integer quantity,

        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.00", message = "商品价格不能小于0")
        @Digits(integer = 8, fraction = 2, message = "商品价格最多8位整数和2位小数")
        BigDecimal price,

        ProductStatus status,

        Integer sortOrder
) {
}
