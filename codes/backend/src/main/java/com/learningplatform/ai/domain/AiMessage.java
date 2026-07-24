package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

public class AiMessage extends BaseEntity {
    private Long conversationId;
    private Long taskId;
    private AiMessageRole role;
    private String content;
    private Integer sequenceNo;
    private Integer tokenCount;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public AiMessageRole getRole() { return role; }
    public void setRole(AiMessageRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
}
