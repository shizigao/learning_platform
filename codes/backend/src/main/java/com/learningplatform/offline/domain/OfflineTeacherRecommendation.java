package com.learningplatform.offline.domain;

import java.time.LocalDateTime;

public class OfflineTeacherRecommendation {
    private Long id;
    private Long taskId;
    private Long userId;
    private String preferenceSnapshot;
    private String candidateSnapshot;
    private String recommendationJson;
    private String inputSnapshotHash;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long value) { taskId = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public String getPreferenceSnapshot() { return preferenceSnapshot; }
    public void setPreferenceSnapshot(String value) { preferenceSnapshot = value; }
    public String getCandidateSnapshot() { return candidateSnapshot; }
    public void setCandidateSnapshot(String value) { candidateSnapshot = value; }
    public String getRecommendationJson() { return recommendationJson; }
    public void setRecommendationJson(String value) { recommendationJson = value; }
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    public void setInputSnapshotHash(String value) { inputSnapshotHash = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
