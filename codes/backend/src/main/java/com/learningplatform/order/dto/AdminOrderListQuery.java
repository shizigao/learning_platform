/* 文件职责：定义管理订单列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.order.domain.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 定义管理订单列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class AdminOrderListQuery extends PageQuery {
    @Size(max = 64, message = "订单号不能超过64个字符")
    /** 保存订单No，供该类型的业务逻辑读取或更新。 */
    private String orderNo;

    @Min(value = 1, message = "用户ID必须大于0")
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;

    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private OrderStatus status;

    /** 返回订单No。 */
    public String getOrderNo() {
        return orderNo;
    }

    /** 更新订单No；调用方仍需遵守所属领域的校验规则。 */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo == null || orderNo.isBlank() ? null : orderNo.trim();
    }

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回状态。 */
    public OrderStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
