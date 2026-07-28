/* 文件职责：定义权益Balances响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

/**
 * 定义权益Balances响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record EntitlementBalancesResponse(
        int aiQuota,
        int examQuota,
        int examOverallAiQuota,
        int examPersonalAiQuota
) {
}
