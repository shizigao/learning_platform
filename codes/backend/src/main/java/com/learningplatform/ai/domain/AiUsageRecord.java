package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

public class AiUsageRecord extends BaseEntity {
    private String businessNo;
    private Long userId;
    private Long taskId;
    private Long entitlementId;
    private AiTaskType usageType;
    private Integer quantity;
    private Integer balanceBefore;
    private Integer balanceAfter;
    private AiUsageStatus status;
    private String remark;

    public String getBusinessNo() { return businessNo; }
    public void setBusinessNo(String businessNo) { this.businessNo = businessNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getEntitlementId() { return entitlementId; }
    public void setEntitlementId(Long entitlementId) { this.entitlementId = entitlementId; }
    public AiTaskType getUsageType() { return usageType; }
    public void setUsageType(AiTaskType usageType) { this.usageType = usageType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(Integer balanceBefore) { this.balanceBefore = balanceBefore; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
    public AiUsageStatus getStatus() { return status; }
    public void setStatus(AiUsageStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
