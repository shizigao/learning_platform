/* 文件职责：表示考试成绩领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示考试成绩领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamResult extends BaseEntity {
    /** 保存考试ID，供该类型的业务逻辑读取或更新。 */
    private Long examId;
    /** 保存作答ID，供该类型的业务逻辑读取或更新。 */
    private Long attemptId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存总计分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal totalScore;
    /** 保存passing分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal passingScore;
    /** 保存passed，供该类型的业务逻辑读取或更新。 */
    private Boolean passed;
    /** 保存correct数量，供该类型的业务逻辑读取或更新。 */
    private Integer correctCount;
    /** 保存incorrect数量，供该类型的业务逻辑读取或更新。 */
    private Integer incorrectCount;
    /** 保存unanswered数量，供该类型的业务逻辑读取或更新。 */
    private Integer unansweredCount;
    /** 保存阅卷Completed，供该类型的业务逻辑读取或更新。 */
    private Boolean gradingCompleted;
    /** 保存visibleTo考生，供该类型的业务逻辑读取或更新。 */
    private Boolean visibleToCandidate;
    /** 保存generated时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime generatedAt;

    /** 返回考试ID。 */
    public Long getExamId() {
        return examId;
    }

    /** 更新考试ID；调用方仍需遵守所属领域的校验规则。 */
    public void setExamId(Long examId) {
        this.examId = examId;
    }

    /** 返回作答ID。 */
    public Long getAttemptId() {
        return attemptId;
    }

    /** 更新作答ID；调用方仍需遵守所属领域的校验规则。 */
    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回总计分数。 */
    public BigDecimal getTotalScore() {
        return totalScore;
    }

    /** 更新总计分数；调用方仍需遵守所属领域的校验规则。 */
    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    /** 返回Passing分数。 */
    public BigDecimal getPassingScore() {
        return passingScore;
    }

    /** 更新Passing分数；调用方仍需遵守所属领域的校验规则。 */
    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    /** 返回Passed。 */
    public Boolean getPassed() {
        return passed;
    }

    /** 更新Passed；调用方仍需遵守所属领域的校验规则。 */
    public void setPassed(Boolean passed) {
        this.passed = passed;
    }

    /** 返回Correct数量。 */
    public Integer getCorrectCount() {
        return correctCount;
    }

    /** 更新Correct数量；调用方仍需遵守所属领域的校验规则。 */
    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    /** 返回Incorrect数量。 */
    public Integer getIncorrectCount() {
        return incorrectCount;
    }

    /** 更新Incorrect数量；调用方仍需遵守所属领域的校验规则。 */
    public void setIncorrectCount(Integer incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    /** 返回Unanswered数量。 */
    public Integer getUnansweredCount() {
        return unansweredCount;
    }

    /** 更新Unanswered数量；调用方仍需遵守所属领域的校验规则。 */
    public void setUnansweredCount(Integer unansweredCount) {
        this.unansweredCount = unansweredCount;
    }

    /** 返回阅卷Completed。 */
    public Boolean getGradingCompleted() {
        return gradingCompleted;
    }

    /** 更新阅卷Completed；调用方仍需遵守所属领域的校验规则。 */
    public void setGradingCompleted(Boolean gradingCompleted) {
        this.gradingCompleted = gradingCompleted;
    }

    /** 返回VisibleTo考生。 */
    public Boolean getVisibleToCandidate() {
        return visibleToCandidate;
    }

    /** 更新VisibleTo考生；调用方仍需遵守所属领域的校验规则。 */
    public void setVisibleToCandidate(Boolean visibleToCandidate) {
        this.visibleToCandidate = visibleToCandidate;
    }

    /** 返回Generated时间。 */
    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    /** 更新Generated时间；调用方仍需遵守所属领域的校验规则。 */
    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
