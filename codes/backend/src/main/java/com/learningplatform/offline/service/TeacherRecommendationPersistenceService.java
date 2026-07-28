/* 文件职责：实现教师推荐持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.service;

import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.mapper.AiTaskMapper;
import com.learningplatform.ai.service.AiQuotaService;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.offline.domain.OfflineTeacherRecommendation;
import com.learningplatform.offline.mapper.OfflineTeachingMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
/**
 * 实现教师推荐持久化业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class TeacherRecommendationPersistenceService {
    /** 保存mapper，供该类型的业务逻辑读取或更新。 */
    private final OfflineTeachingMapper mapper;
    /** 访问任务持久化数据。 */
    private final AiTaskMapper taskMapper;
    /** 委托额度执行对应领域规则。 */
    private final AiQuotaService quotaService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public TeacherRecommendationPersistenceService(
            OfflineTeachingMapper mapper,
            AiTaskMapper taskMapper,
            AiQuotaService quotaService
    ) {
        this.mapper = mapper;
        this.taskMapper = taskMapper;
        this.quotaService = quotaService;
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public OfflineTeacherRecommendation save(
            AiTask task,
            OfflineTeacherRecommendation recommendation
    ) {
        recommendation.setTaskId(task.getId());
        recommendation.setUserId(task.getUserId());
        if (mapper.insertRecommendation(recommendation) != 1) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "保存教师推荐结果失败"
            );
        }
        quotaService.consume(task);
        if (taskMapper.markSucceeded(
                task.getId(),
                LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
        ) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "AI 任务状态已变化");
        }
        return mapper.findRecommendationByTaskId(task.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INTERNAL_ERROR,
                        "保存后无法读取教师推荐结果"
                ));
    }
}
