package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

public class AiSummary extends BaseEntity {
    private Long taskId;
    private Long contentId;
    private String summaryText;
    private String knowledgePointsJson;
    private String reviewOutline;
    private String sourceVersion;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getKnowledgePointsJson() { return knowledgePointsJson; }
    public void setKnowledgePointsJson(String knowledgePointsJson) { this.knowledgePointsJson = knowledgePointsJson; }
    public String getReviewOutline() { return reviewOutline; }
    public void setReviewOutline(String reviewOutline) { this.reviewOutline = reviewOutline; }
    public String getSourceVersion() { return sourceVersion; }
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
}
