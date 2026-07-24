package com.learningplatform.order.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.order.domain.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AdminOrderListQuery extends PageQuery {
    @Size(max = 64, message = "订单号不能超过64个字符")
    private String orderNo;

    @Min(value = 1, message = "用户ID必须大于0")
    private Long userId;

    private OrderStatus status;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo == null || orderNo.isBlank() ? null : orderNo.trim();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
