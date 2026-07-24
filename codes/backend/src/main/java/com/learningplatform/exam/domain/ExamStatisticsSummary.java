package com.learningplatform.exam.domain;

import java.math.BigDecimal;

public class ExamStatisticsSummary {
    private Integer totalCandidates;
    private Integer participatedCount;
    private Integer submittedCount;
    private Integer notParticipatedCount;
    private Integer gradedCount;
    private BigDecimal averageScore;
    private BigDecimal highestScore;
    private BigDecimal lowestScore;
    private Integer passedCount;

    public Integer getTotalCandidates() {
        return totalCandidates;
    }

    public void setTotalCandidates(Integer totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    public Integer getParticipatedCount() {
        return participatedCount;
    }

    public void setParticipatedCount(Integer participatedCount) {
        this.participatedCount = participatedCount;
    }

    public Integer getSubmittedCount() {
        return submittedCount;
    }

    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    public Integer getNotParticipatedCount() {
        return notParticipatedCount;
    }

    public void setNotParticipatedCount(Integer notParticipatedCount) {
        this.notParticipatedCount = notParticipatedCount;
    }

    public Integer getGradedCount() {
        return gradedCount;
    }

    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    public BigDecimal getHighestScore() {
        return highestScore;
    }

    public void setHighestScore(BigDecimal highestScore) {
        this.highestScore = highestScore;
    }

    public BigDecimal getLowestScore() {
        return lowestScore;
    }

    public void setLowestScore(BigDecimal lowestScore) {
        this.lowestScore = lowestScore;
    }

    public Integer getPassedCount() {
        return passedCount;
    }

    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }
}
