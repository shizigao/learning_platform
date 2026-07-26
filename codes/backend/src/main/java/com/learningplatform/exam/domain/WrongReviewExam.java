package com.learningplatform.exam.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WrongReviewExam {
    private Long resultId;
    private Long examId;
    private Long attemptId;
    private String examName;
    private BigDecimal fullScore;
    private BigDecimal totalScore;
    private BigDecimal passingScore;
    private Boolean passed;
    private Boolean answersVisible;
    private LocalDateTime generatedAt;

    public Long getResultId() {
        return resultId;
    }

    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public BigDecimal getFullScore() {
        return fullScore;
    }

    public void setFullScore(BigDecimal fullScore) {
        this.fullScore = fullScore;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public Boolean getPassed() {
        return passed;
    }

    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    public Boolean getAnswersVisible() {
        return answersVisible;
    }

    public void setAnswersVisible(Boolean answersVisible) {
        this.answersVisible = answersVisible;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
