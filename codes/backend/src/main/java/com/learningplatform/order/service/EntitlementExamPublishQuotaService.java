/* 文件职责：实现权益考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：商品、订单、支付模拟与用户权益；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.order.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.exam.service.ExamPublishQuotaService;
import com.learningplatform.order.domain.EntitlementType;
import org.springframework.stereotype.Service;

@Service
/**
 * 实现权益考试发布额度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class EntitlementExamPublishQuotaService implements ExamPublishQuotaService {
    /** 委托权益执行对应领域规则。 */
    private final EntitlementService entitlementService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public EntitlementExamPublishQuotaService(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @Override
    /** 执行消费核心计算或业务处理，并保证失败不会留下不一致的持久化结果。 */
    public void consume(Long publisherId, Long examId) {
        try {
            entitlementService.consumeQuota(
                    publisherId,
                    EntitlementType.EXAM_QUOTA,
                    1
            );
        } catch (BusinessException exception) {
            if (exception.getErrorCode() == ErrorCode.FORBIDDEN) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "考试发布额度不足");
            }
            throw exception;
        }
    }

    @Override
    /** 执行 availableQuota 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public int availableQuota(Long publisherId) {
        return entitlementService.availableQuota(
                publisherId,
                EntitlementType.EXAM_QUOTA
        );
    }
}
