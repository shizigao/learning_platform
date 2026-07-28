/* 文件职责：表示BusinessNumberGenerator领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
/**
 * 表示BusinessNumberGenerator领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class BusinessNumberGenerator {
    /** 执行 nextOrderNo 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String nextOrderNo() {
        return next("ORD");
    }

    /** 执行 nextPaymentNo 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String nextPaymentNo() {
        return next("PAY");
    }

    /** 执行 nextMockTransactionNo 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public String nextMockTransactionNo() {
        return next("MOCK");
    }

    /** 执行 next 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String next(String prefix) {
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return prefix + random.substring(0, 24);
    }
}
