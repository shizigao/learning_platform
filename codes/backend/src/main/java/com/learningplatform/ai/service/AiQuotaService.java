package com.learningplatform.ai.service;

import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiUsageRecord;
import com.learningplatform.ai.domain.AiUsageStatus;
import com.learningplatform.ai.dto.AiUsageRecordResponse;
import com.learningplatform.ai.mapper.AiUsageRecordMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.EntitlementType;
import com.learningplatform.order.domain.UserEntitlement;
import com.learningplatform.order.mapper.UserEntitlementMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AiQuotaService {
    private final UserEntitlementMapper entitlementMapper;
    private final AiUsageRecordMapper usageMapper;

    public AiQuotaService(
            UserEntitlementMapper entitlementMapper,
            AiUsageRecordMapper usageMapper
    ) {
        this.entitlementMapper = entitlementMapper;
        this.usageMapper = usageMapper;
    }

    public void requireAvailable(Long userId, int quantity) {
        if (quantity <= 0
                || entitlementMapper.sumAvailableQuota(
                        userId,
                        EntitlementType.AI_QUOTA
                ) < quantity) {
            throw noQuota();
        }
    }

    @Transactional
    public AiUsageRecord consume(AiTask task) {
        String businessNo = businessNo(task.getId());
        AiUsageRecord existing = usageMapper.findByBusinessNo(businessNo)
                .orElse(null);
        if (existing != null) {
            return existing;
        }
        int quantity = task.getQuotaCost() == null ? 1 : task.getQuotaCost();
        if (quantity != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 次数扣减配置无效"
            );
        }
        UserEntitlement entitlement = entitlementMapper
                .findAvailableQuotaForUpdate(
                        task.getUserId(),
                        EntitlementType.AI_QUOTA
                )
                .orElseThrow(this::noQuota);
        int balanceBefore = entitlementMapper.sumAvailableQuota(
                task.getUserId(),
                EntitlementType.AI_QUOTA
        );
        if (balanceBefore < quantity
                || entitlementMapper.consume(
                        entitlement.getId(),
                        entitlement.getVersion(),
                        quantity
                ) != 1) {
            throw noQuota();
        }
        int balanceAfter = entitlementMapper.sumAvailableQuota(
                task.getUserId(),
                EntitlementType.AI_QUOTA
        );

        AiUsageRecord record = new AiUsageRecord();
        record.setBusinessNo(businessNo);
        record.setUserId(task.getUserId());
        record.setTaskId(task.getId());
        record.setEntitlementId(entitlement.getId());
        record.setUsageType(task.getTaskType());
        record.setQuantity(quantity);
        record.setBalanceBefore(balanceBefore);
        record.setBalanceAfter(balanceAfter);
        record.setStatus(AiUsageStatus.CONSUMED);
        record.setRemark("AI 任务成功完成并保存结果");
        if (usageMapper.insert(record) != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "保存 AI 使用记录失败"
            );
        }
        return usageMapper.findByBusinessNo(businessNo)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存后无法读取 AI 使用记录"
                ));
    }

    public List<AiUsageRecordResponse> records(Long userId) {
        return usageMapper.findByUserId(userId).stream()
                .map(AiUsageRecordResponse::from)
                .toList();
    }

    private String businessNo(Long taskId) {
        return "AI_TASK_" + taskId;
    }

    private BusinessException noQuota() {
        return new BusinessException(
                ErrorCode.FORBIDDEN,
                "AI 可用次数不足，请先购买 AI 次数包"
        );
    }
}
