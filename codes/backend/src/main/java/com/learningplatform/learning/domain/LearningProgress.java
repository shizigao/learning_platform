package com.learningplatform.learning.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LearningProgress extends BaseEntity {
    private Long userId;
    private Long contentId;
    private LocalDateTime startedAt;
    private LocalDateTime lastLearnedAt;
    private BigDecimal progressPercent;
    private String lastPosition;
    private LocalDateTime completedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getContentId() {
        return contentId;
    }

    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastLearnedAt() {
        return lastLearnedAt;
    }

    public void setLastLearnedAt(LocalDateTime lastLearnedAt) {
        this.lastLearnedAt = lastLearnedAt;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(String lastPosition) {
        this.lastPosition = lastPosition;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
