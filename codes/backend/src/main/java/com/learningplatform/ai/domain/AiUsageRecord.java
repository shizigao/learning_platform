/* 文件职责：表示AI用量记录领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示AI用量记录领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class AiUsageRecord extends BaseEntity {
    /** 保存businessNo，供该类型的业务逻辑读取或更新。 */
    private String businessNo;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存任务ID，供该类型的业务逻辑读取或更新。 */
    private Long taskId;
    /** 保存权益ID，供该类型的业务逻辑读取或更新。 */
    private Long entitlementId;
    /** 保存用量类型，供该类型的业务逻辑读取或更新。 */
    private AiTaskType usageType;
    /** 保存quantity，供该类型的业务逻辑读取或更新。 */
    private Integer quantity;
    /** 保存balanceBefore，供该类型的业务逻辑读取或更新。 */
    private Integer balanceBefore;
    /** 保存balanceAfter，供该类型的业务逻辑读取或更新。 */
    private Integer balanceAfter;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private AiUsageStatus status;
    /** 保存remark，供该类型的业务逻辑读取或更新。 */
    private String remark;

    /** 返回BusinessNo。 */
    public String getBusinessNo() { return businessNo; }
    /** 更新BusinessNo；调用方仍需遵守所属领域的校验规则。 */
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) { this.userId = userId; }
    /** 返回任务ID。 */
    public Long getTaskId() { return taskId; }
    /** 更新任务ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    /** 返回权益ID。 */
    public Long getEntitlementId() { return entitlementId; }
    /** 更新权益ID；调用方仍需遵守所属领域的校验规则。 */
    public void setEntitlementId(Long entitlementId) { this.entitlementId = entitlementId; }
    /** 返回用量类型。 */
    public AiTaskType getUsageType() { return usageType; }
    /** 更新用量类型；调用方仍需遵守所属领域的校验规则。 */
    public void setUsageType(AiTaskType usageType) { this.usageType = usageType; }
    /** 返回Quantity。 */
    public Integer getQuantity() { return quantity; }
    /** 更新Quantity；调用方仍需遵守所属领域的校验规则。 */
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    /** 返回BalanceBefore。 */
    public Integer getBalanceBefore() { return balanceBefore; }
    /** 更新BalanceBefore；调用方仍需遵守所属领域的校验规则。 */
    public void setBalanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; }
    /** 返回BalanceAfter。 */
    public Integer getBalanceAfter() { return balanceAfter; }
    /** 更新BalanceAfter；调用方仍需遵守所属领域的校验规则。 */
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    /** 返回状态。 */
    public AiUsageStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(AiUsageStatus status) { this.status = status; }
    /** 返回Remark。 */
    public String getRemark() { return remark; }
    /** 更新Remark；调用方仍需遵守所属领域的校验规则。 */
    public void setRemark(String remark) { this.remark = remark; }
}
