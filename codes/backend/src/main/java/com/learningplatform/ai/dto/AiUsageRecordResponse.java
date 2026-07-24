package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.domain.AiUsageRecord;
import com.learningplatform.ai.domain.AiUsageStatus;

import java.time.LocalDateTime;

public record AiUsageRecordResponse(
        Long id,
        String businessNo,
        Long taskId,
        Long entitlementId,
        AiTaskType usageType,
        Integer quantity,
        Integer balanceBefore,
        Integer balanceAfter,
        AiUsageStatus status,
        String remark,
        LocalDateTime createdAt
) {
    public static AiUsageRecordResponse from(AiUsageRecord record) {
        return new AiUsageRecordResponse(
                record.getId(),
                record.getBusinessNo(),
                record.getTaskId(),
                record.getEntitlementId(),
                record.getUsageType(),
                record.getQuantity(),
                record.getBalanceBefore(),
                record.getBalanceAfter(),
                record.getStatus(),
                record.getRemark(),
                record.getCreatedAt()
        );
    }
}
