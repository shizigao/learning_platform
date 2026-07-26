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
public class TeacherRecommendationPersistenceService {
    private final OfflineTeachingMapper mapper;
    private final AiTaskMapper taskMapper;
    private final AiQuotaService quotaService;

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
