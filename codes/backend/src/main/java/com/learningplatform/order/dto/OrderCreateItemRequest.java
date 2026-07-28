/* 文件职责：定义订单创建Item请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 定义订单创建Item请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record OrderCreateItemRequest(
        @NotNull(message = "商品ID不能为空")
        @Min(value = 1, message = "商品ID必须大于0")
        Long productId,

        @Min(value = 1, message = "购买数量必须大于0")
        @Max(value = 99, message = "单项购买数量不能超过99")
        int quantity
) {
}
