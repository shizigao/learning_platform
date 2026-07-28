/* 文件职责：定义考试AI分析响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.ExamAiAnalysis;
import com.learningplatform.ai.domain.ExamAiAnalysisScope;

import java.time.LocalDateTime;

/**
 * 定义考试AI分析响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamAiAnalysisResponse(
        Long id,
        AiTaskResponse task,
        Long examId,
        Long attemptId,
        ExamAiAnalysisScope scope,
        String reportMarkdown,
        String inputSnapshotHash,
        LocalDateTime createdAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamAiAnalysisResponse from(
            ExamAiAnalysis analysis,
            AiTaskResponse task
    ) {
        return new ExamAiAnalysisResponse(
                analysis.getId(),
                task,
                analysis.getExamId(),
                analysis.getAttemptId(),
                analysis.getAnalysisScope(),
                analysis.getReportMarkdown(),
                analysis.getInputSnapshotHash(),
                analysis.getCreatedAt()
        );
    }
}
