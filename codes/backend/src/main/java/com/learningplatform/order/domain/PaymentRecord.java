/* 文件职责：表示支付记录领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示支付记录领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class PaymentRecord extends BaseEntity {
    /** 保存支付No，供该类型的业务逻辑读取或更新。 */
    private String paymentNo;
    /** 保存订单ID，供该类型的业务逻辑读取或更新。 */
    private Long orderId;
    /** 保存供应商，供该类型的业务逻辑读取或更新。 */
    private PaymentProvider provider;
    /** 保存供应商TransactionNo，供该类型的业务逻辑读取或更新。 */
    private String providerTransactionNo;
    /** 保存amount，供该类型的业务逻辑读取或更新。 */
    private BigDecimal amount;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private PaymentStatus status;
    /** 保存请求Payload，供该类型的业务逻辑读取或更新。 */
    private String requestPayload;
    /** 保存响应Payload，供该类型的业务逻辑读取或更新。 */
    private String responsePayload;
    /** 保存failure原因，供该类型的业务逻辑读取或更新。 */
    private String failureReason;
    /** 保存paid时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime paidAt;

    /** 返回支付No。 */
    public String getPaymentNo() {
        return paymentNo;
    }

    /** 更新支付No；调用方仍需遵守所属领域的校验规则。 */
    public void setPaymentNo(String paymentNo) {
        this.paymentNo = paymentNo;
    }

    /** 返回订单ID。 */
    public Long getOrderId() {
        return orderId;
    }

    /** 更新订单ID；调用方仍需遵守所属领域的校验规则。 */
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    /** 返回供应商。 */
    public PaymentProvider getProvider() {
        return provider;
    }

    /** 更新供应商；调用方仍需遵守所属领域的校验规则。 */
    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    /** 返回供应商TransactionNo。 */
    public String getProviderTransactionNo() {
        return providerTransactionNo;
    }

    /** 更新供应商TransactionNo；调用方仍需遵守所属领域的校验规则。 */
    public void setProviderTransactionNo(String providerTransactionNo) {
        this.providerTransactionNo = providerTransactionNo;
    }

    /** 返回Amount。 */
    public BigDecimal getAmount() {
        return amount;
    }

    /** 更新Amount；调用方仍需遵守所属领域的校验规则。 */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /** 返回状态。 */
    public PaymentStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    /** 返回请求Payload。 */
    public String getRequestPayload() {
        return requestPayload;
    }

    /** 更新请求Payload；调用方仍需遵守所属领域的校验规则。 */
    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    /** 返回响应Payload。 */
    public String getResponsePayload() {
        return responsePayload;
    }

    /** 更新响应Payload；调用方仍需遵守所属领域的校验规则。 */
    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    /** 返回Failure原因。 */
    public String getFailureReason() {
        return failureReason;
    }

    /** 更新Failure原因；调用方仍需遵守所属领域的校验规则。 */
    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    /** 返回Paid时间。 */
    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    /** 更新Paid时间；调用方仍需遵守所属领域的校验规则。 */
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
