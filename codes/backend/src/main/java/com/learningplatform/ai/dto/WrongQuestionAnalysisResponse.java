package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.WrongQuestionAnalysis;

import java.time.LocalDateTime;

public record WrongQuestionAnalysisResponse(
        Long id,
        AiTaskResponse task,
        int examCount,
        int questionCount,
        String reportMarkdown,
        String inputSnapshotHash,
        LocalDateTime createdAt
) {
    public static WrongQuestionAnalysisResponse from(
            WrongQuestionAnalysis analysis,
            AiTaskResponse task
    ) {
        return new WrongQuestionAnalysisResponse(
                analysis.getId(),
                task,
                analysis.getExamCount(),
                analysis.getQuestionCount(),
                analysis.getReportMarkdown(),
                analysis.getInputSnapshotHash(),
                analysis.getCreatedAt()
        );
    }
}
