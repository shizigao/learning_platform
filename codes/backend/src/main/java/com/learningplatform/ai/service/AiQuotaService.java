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
        requireAvailable(userId, EntitlementType.AI_QUOTA, quantity);
    }

    public void requireAvailable(
            Long userId,
            EntitlementType entitlementType,
            int quantity
    ) {
        if (quantity <= 0
                || entitlementMapper.sumAvailableQuota(
                        userId,
                        entitlementType
                ) < quantity) {
            throw noQuota(entitlementType);
        }
    }

    @Transactional
    public AiUsageRecord consume(AiTask task) {
        return consume(task, EntitlementType.AI_QUOTA);
    }

    @Transactional
    public AiUsageRecord consume(
            AiTask task,
            EntitlementType entitlementType
    ) {
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
                        entitlementType
                )
                .orElseThrow(() -> noQuota(entitlementType));
        int balanceBefore = entitlementMapper.sumAvailableQuota(
                task.getUserId(),
                entitlementType
        );
        if (balanceBefore < quantity
                || entitlementMapper.consume(
                        entitlement.getId(),
                        entitlement.getVersion(),
                        quantity
                ) != 1) {
            throw noQuota(entitlementType);
        }
        int balanceAfter = entitlementMapper.sumAvailableQuota(
                task.getUserId(),
                entitlementType
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

    private BusinessException noQuota(EntitlementType entitlementType) {
        String message = switch (entitlementType) {
            case EXAM_OVERALL_AI_QUOTA -> "考试整体 AI 分析次数不足，请先购买次数包";
            case EXAM_PERSONAL_AI_QUOTA -> "考试个人 AI 分析次数不足，请先购买次数包";
            default -> "AI 可用次数不足，请先购买 AI 次数包";
        };
        return new BusinessException(
                ErrorCode.FORBIDDEN,
                message
        );
    }
}
