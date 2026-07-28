/* 文件职责：实现考试AI分析持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.ExamAiAnalysis;
import com.learningplatform.ai.mapper.AiTaskMapper;
import com.learningplatform.ai.mapper.ExamAiAnalysisMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.order.domain.EntitlementType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
/**
 * 实现考试AI分析持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamAiAnalysisPersistenceService {
    /** 访问analysis持久化数据。 */
    private final ExamAiAnalysisMapper analysisMapper;
    /** 访问任务持久化数据。 */
    private final AiTaskMapper taskMapper;
    /** 委托额度执行对应领域规则。 */
    private final AiQuotaService quotaService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamAiAnalysisPersistenceService(
            ExamAiAnalysisMapper analysisMapper,
            AiTaskMapper taskMapper,
            AiQuotaService quotaService
    ) {
        this.analysisMapper = analysisMapper;
        this.taskMapper = taskMapper;
        this.quotaService = quotaService;
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public ExamAiAnalysis save(
            AiTask task,
            ExamAiAnalysis analysis,
            EntitlementType entitlementType
    ) {
        analysis.setTaskId(task.getId());
        if (analysisMapper.insert(analysis) != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "保存考试 AI 分析报告失败"
            );
        }
        quotaService.consume(task, entitlementType);
        if (taskMapper.markSucceeded(
                task.getId(),
                LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 任务状态已发生变化");
        }
        return analysisMapper.findByTaskId(task.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存后无法读取考试 AI 分析报告"
                ));
    }
}
