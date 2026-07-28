/* 文件职责：实现AI额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现AI额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AiQuotaService {
    /** 访问权益持久化数据。 */
    private final UserEntitlementMapper entitlementMapper;
    /** 访问用量持久化数据。 */
    private final AiUsageRecordMapper usageMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiQuotaService(
            UserEntitlementMapper entitlementMapper,
            AiUsageRecordMapper usageMapper
    ) {
        this.entitlementMapper = entitlementMapper;
        this.usageMapper = usageMapper;
    }

    /** 校验可用及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void requireAvailable(Long userId, int quantity) {
        requireAvailable(userId, EntitlementType.AI_QUOTA, quantity);
    }

    /** 校验可用及相关业务前置条件，不满足时抛出明确业务异常。 */
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
    /** 执行消费核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public AiUsageRecord consume(AiTask task) {
        return consume(task, EntitlementType.AI_QUOTA);
    }

    @Transactional
    /** 执行消费核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
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

    /** 执行 records 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public List<AiUsageRecordResponse> records(Long userId) {
        return usageMapper.findByUserId(userId).stream()
                .map(AiUsageRecordResponse::from)
                .toList();
    }

    /** 执行 businessNo 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String businessNo(Long taskId) {
        return "AI_TASK_" + taskId;
    }

    /** 执行 noQuota 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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
