/* 文件职责：表示考试考生领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * 表示考试考生领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamCandidate extends BaseEntity {
    /** 保存考试ID，供该类型的业务逻辑读取或更新。 */
    private Long examId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存username，供该类型的业务逻辑读取或更新。 */
    private String username;
    /** 保存nickname，供该类型的业务逻辑读取或更新。 */
    private String nickname;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ExamCandidateStatus status;
    /** 保存assigned时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime assignedAt;
    /** 保存started时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime startedAt;
    /** 保存提交时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime submittedAt;

    /** 返回考试ID。 */
    public Long getExamId() {
        return examId;
    }

    /** 更新考试ID；调用方仍需遵守所属领域的校验规则。 */
    public void setExamId(Long examId) {
        this.examId = examId;
    }

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
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

    /** 返回状态。 */
    public ExamCandidateStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ExamCandidateStatus status) {
        this.status = status;
    }

    /** 返回Assigned时间。 */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    /** 更新Assigned时间；调用方仍需遵守所属领域的校验规则。 */
    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    /** 返回Started时间。 */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** 更新Started时间；调用方仍需遵守所属领域的校验规则。 */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /** 返回提交时间。 */
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    /** 更新提交时间；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
}
