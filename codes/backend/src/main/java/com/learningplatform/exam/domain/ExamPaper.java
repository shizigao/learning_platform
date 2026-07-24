package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

public class ExamPaper extends BaseEntity {
    private Long creatorId;
    private String name;
    private String description;
    private BigDecimal totalScore;
    private Integer questionCount;
    private ExamPaperStatus status;

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
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

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public ExamPaperStatus getStatus() {
        return status;
    }

    public void setStatus(ExamPaperStatus status) {
        this.status = status;
    }
}
