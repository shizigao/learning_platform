package com.learningplatform.order.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.order.domain.OrderStatus;

public class OrderListQuery extends PageQuery {
    private OrderStatus status;

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}
