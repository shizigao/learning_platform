package com.learningplatform.question.domain;

import com.learningplatform.common.model.BaseEntity;

public class QuestionBank extends BaseEntity {
    private Long ownerId;
    private String name;
    private String description;
    private QuestionStatus status;

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
