/* 文件职责：定义AI用量记录响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiTaskType;
import com.learningplatform.ai.domain.AiUsageRecord;
import com.learningplatform.ai.domain.AiUsageStatus;

import java.time.LocalDateTime;

/**
 * 定义AI用量记录响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
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
    /** 转换或规范化数据，不引入额外持久化副作用。 */
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
