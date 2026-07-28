/* 文件职责：定义订单创建请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 定义订单创建请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
