package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

public class ExamAiAnalysis extends BaseEntity {
    private Long taskId;
    private Long examId;
    private Long attemptId;
    private Long requesterId;
    private ExamAiAnalysisScope analysisScope;
    private String reportMarkdown;
    private String inputSnapshotHash;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getRequesterId() { return requesterId; }
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    public ExamAiAnalysisScope getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(ExamAiAnalysisScope analysisScope) {
        this.analysisScope = analysisScope;
    }
    public String getReportMarkdown() { return reportMarkdown; }
    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
    }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public void setInputSnapshotHash(String inputSnapshotHash) {
        this.inputSnapshotHash = inputSnapshotHash;
    }
}
