package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamAttempt extends BaseEntity {
    private Long examId;
    private Long candidateId;
    private Long userId;
    private Integer attemptNo;
    private ExamAttemptStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime deadlineAt;
    private LocalDateTime lastSavedAt;
    private LocalDateTime submittedAt;
    private ExamSubmissionType submissionType;
    private BigDecimal objectiveScore;
    private BigDecimal subjectiveScore;
    private BigDecimal finalScore;
    private Integer version;
    private String username;
    private String nickname;
    private Integer pendingReviewCount;
    private Boolean gradingCompleted;

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getAttemptNo() {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    public ExamAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(ExamAttemptStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public ExamSubmissionType getSubmissionType() {
        return submissionType;
    }

    public void setSubmissionType(ExamSubmissionType submissionType) {
        this.submissionType = submissionType;
    }

    public BigDecimal getObjectiveScore() {
        return objectiveScore;
    }

    public void setObjectiveScore(BigDecimal objectiveScore) {
        this.objectiveScore = objectiveScore;
    }

    public BigDecimal getSubjectiveScore() {
        return subjectiveScore;
    }

    public void setSubjectiveScore(BigDecimal subjectiveScore) {
        this.subjectiveScore = subjectiveScore;
    }

    public BigDecimal getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Boolean getGradingCompleted() {
        return gradingCompleted;
    }

    public void setGradingCompleted(Boolean gradingCompleted) {
        this.gradingCompleted = gradingCompleted;
    }
}
