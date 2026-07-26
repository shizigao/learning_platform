package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

public class WrongQuestionAnalysis extends BaseEntity {
    private Long taskId;
    private Long requesterId;
    private Integer examCount;
    private Integer questionCount;
    private String reportMarkdown;
    private String inputSnapshotHash;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getRequesterId() {
        return requesterId;
    }

    public void setRequesterId(Long requesterId) {
        this.requesterId = requesterId;
    }

    public Integer getExamCount() {
        return examCount;
    }

    public void setExamCount(Integer examCount) {
        this.examCount = examCount;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public String getReportMarkdown() {
        return reportMarkdown;
    }

    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
    }

    public String getInputSnapshotHash() {
        return inputSnapshotHash;
    }

    public void setInputSnapshotHash(String inputSnapshotHash) {
        this.inputSnapshotHash = inputSnapshotHash;
    }
}
