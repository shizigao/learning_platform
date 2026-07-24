package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamResult extends BaseEntity {
    private Long examId;
    private Long attemptId;
    private Long userId;
    private BigDecimal totalScore;
    private BigDecimal passingScore;
    private Boolean passed;
    private Integer correctCount;
    private Integer incorrectCount;
    private Integer unansweredCount;
    private Boolean gradingCompleted;
    private Boolean visibleToCandidate;
    private LocalDateTime generatedAt;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(Integer incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public Integer getUnansweredCount() {
        return unansweredCount;
    }

    public void setUnansweredCount(Integer unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    public Boolean getGradingCompleted() {
        return gradingCompleted;
    }

    public void setGradingCompleted(Boolean gradingCompleted) {
        this.gradingCompleted = gradingCompleted;
    }

    public Boolean getVisibleToCandidate() {
        return visibleToCandidate;
    }

    public void setVisibleToCandidate(Boolean visibleToCandidate) {
        this.visibleToCandidate = visibleToCandidate;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
