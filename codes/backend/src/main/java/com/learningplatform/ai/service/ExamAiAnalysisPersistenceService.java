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
public class ExamAiAnalysisPersistenceService {
    private final ExamAiAnalysisMapper analysisMapper;
    private final AiTaskMapper taskMapper;
    private final AiQuotaService quotaService;

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
