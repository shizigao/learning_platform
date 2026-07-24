package com.learningplatform.question.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

public class Question extends BaseEntity {
    private Long bankId;
    private Long creatorId;
    private QuestionType questionType;
    private String stem;
    private String answerJson;
    private String answerText;
    private String analysis;
    private BigDecimal defaultScore;
    private Boolean fillBlankAutoGradable;
    private Boolean caseSensitive;
    private QuestionStatus status;

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getStem() {
        return stem;
    }

    public void setStem(String stem) {
        this.stem = stem;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    public BigDecimal getDefaultScore() {
        return defaultScore;
    }

    public void setDefaultScore(BigDecimal defaultScore) {
        this.defaultScore = defaultScore;
    }

    public Boolean getFillBlankAutoGradable() {
        return fillBlankAutoGradable;
    }

    public void setFillBlankAutoGradable(Boolean fillBlankAutoGradable) {
        this.fillBlankAutoGradable = fillBlankAutoGradable;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
