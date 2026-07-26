package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.ExamAiAnalysis;
import com.learningplatform.ai.domain.ExamAiAnalysisScope;

import java.time.LocalDateTime;

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
