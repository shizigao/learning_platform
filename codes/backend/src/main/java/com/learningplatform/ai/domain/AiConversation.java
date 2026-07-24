package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

public class AiConversation extends BaseEntity {
    private Long userId;
    private Long contentId;
    private String title;
    private AiConversationStatus status;
    private LocalDateTime lastMessageAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AiConversationStatus getStatus() { return status; }
    public void setStatus(AiConversationStatus status) { this.status = status; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
