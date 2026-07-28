/* 文件职责：表示考试作答领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示考试作答领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamAttempt extends BaseEntity {
    /** 保存考试ID，供该类型的业务逻辑读取或更新。 */
    private Long examId;
    /** 保存考生ID，供该类型的业务逻辑读取或更新。 */
    private Long candidateId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存作答No，供该类型的业务逻辑读取或更新。 */
    private Integer attemptNo;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ExamAttemptStatus status;
    /** 保存started时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime startedAt;
    /** 保存deadline时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime deadlineAt;
    /** 保存lastSaved时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime lastSavedAt;
    /** 保存提交时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime submittedAt;
    /** 保存交卷类型，供该类型的业务逻辑读取或更新。 */
    private ExamSubmissionType submissionType;
    /** 保存objective分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal objectiveScore;
    /** 保存subjective分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal subjectiveScore;
    /** 保存final分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal finalScore;
    /** 保存version，供该类型的业务逻辑读取或更新。 */
    private Integer version;
    /** 保存username，供该类型的业务逻辑读取或更新。 */
    private String username;
    /** 保存nickname，供该类型的业务逻辑读取或更新。 */
    private String nickname;
    /** 保存pending复习数量，供该类型的业务逻辑读取或更新。 */
    private Integer pendingReviewCount;
    /** 保存阅卷Completed，供该类型的业务逻辑读取或更新。 */
    private Boolean gradingCompleted;

    /** 返回考试ID。 */
    public Long getExamId() {
        return examId;
    }

    /** 更新考试ID；调用方仍需遵守所属领域的校验规则。 */
    public void setExamId(Long examId) {
        this.examId = examId;
    }

    /** 返回考生ID。 */
    public Long getCandidateId() {
        return candidateId;
    }

    /** 更新考生ID；调用方仍需遵守所属领域的校验规则。 */
    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回作答No。 */
    public Integer getAttemptNo() {
        return attemptNo;
    }

    /** 更新作答No；调用方仍需遵守所属领域的校验规则。 */
    public void setAttemptNo(Integer attemptNo) {
        this.attemptNo = attemptNo;
    }

    /** 返回状态。 */
    public ExamAttemptStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ExamAttemptStatus status) {
        this.status = status;
    }

    /** 返回Started时间。 */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** 更新Started时间；调用方仍需遵守所属领域的校验规则。 */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /** 返回Deadline时间。 */
    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    /** 更新Deadline时间；调用方仍需遵守所属领域的校验规则。 */
    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    /** 返回LastSaved时间。 */
    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    /** 更新LastSaved时间；调用方仍需遵守所属领域的校验规则。 */
    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    /** 返回提交时间。 */
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    /** 更新提交时间；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /** 返回交卷类型。 */
    public ExamSubmissionType getSubmissionType() {
        return submissionType;
    }

    /** 更新交卷类型；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmissionType(ExamSubmissionType submissionType) {
        this.submissionType = submissionType;
    }

    /** 返回Objective分数。 */
    public BigDecimal getObjectiveScore() {
        return objectiveScore;
    }

    /** 更新Objective分数；调用方仍需遵守所属领域的校验规则。 */
    public void setObjectiveScore(BigDecimal objectiveScore) {
        this.objectiveScore = objectiveScore;
    }

    /** 返回Subjective分数。 */
    public BigDecimal getSubjectiveScore() {
        return subjectiveScore;
    }

    /** 更新Subjective分数；调用方仍需遵守所属领域的校验规则。 */
    public void setSubjectiveScore(BigDecimal subjectiveScore) {
        this.subjectiveScore = subjectiveScore;
    }

    /** 返回Final分数。 */
    public BigDecimal getFinalScore() {
        return finalScore;
    }

    /** 更新Final分数；调用方仍需遵守所属领域的校验规则。 */
    public void setFinalScore(BigDecimal finalScore) {
        this.finalScore = finalScore;
    }

    /** 返回Version。 */
    public Integer getVersion() {
        return version;
    }

    /** 更新Version；调用方仍需遵守所属领域的校验规则。 */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /** 返回Username。 */
    public String getUsername() {
        return username;
    }

    /** 更新Username；调用方仍需遵守所属领域的校验规则。 */
    public void setUsername(String username) {
        this.username = username;
    }

    /** 返回Nickname。 */
    public String getNickname() {
        return nickname;
    }

    /** 更新Nickname；调用方仍需遵守所属领域的校验规则。 */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 返回Pending复习数量。 */
    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    /** 更新Pending复习数量；调用方仍需遵守所属领域的校验规则。 */
    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    /** 返回阅卷Completed。 */
    public Boolean getGradingCompleted() {
        return gradingCompleted;
    }

    /** 更新阅卷Completed；调用方仍需遵守所属领域的校验规则。 */
    public void setGradingCompleted(Boolean gradingCompleted) {
        this.gradingCompleted = gradingCompleted;
    }
}
