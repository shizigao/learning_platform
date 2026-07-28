/* 文件职责：表示错题复习考试领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示错题复习考试领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class WrongReviewExam {
    /** 保存成绩ID，供该类型的业务逻辑读取或更新。 */
    private Long resultId;
    /** 保存考试ID，供该类型的业务逻辑读取或更新。 */
    private Long examId;
    /** 保存作答ID，供该类型的业务逻辑读取或更新。 */
    private Long attemptId;
    /** 保存考试名称，供该类型的业务逻辑读取或更新。 */
    private String examName;
    /** 保存full分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal fullScore;
    /** 保存总计分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal totalScore;
    /** 保存passing分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal passingScore;
    /** 保存passed，供该类型的业务逻辑读取或更新。 */
    private Boolean passed;
    /** 保存answersVisible，供该类型的业务逻辑读取或更新。 */
    private Boolean answersVisible;
    /** 保存generated时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime generatedAt;

    /** 返回成绩ID。 */
    public Long getResultId() {
        return resultId;
    }

    /** 更新成绩ID；调用方仍需遵守所属领域的校验规则。 */
    public void setResultId(Long resultId) {
        this.resultId = resultId;
    }

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

    /** 返回考试名称。 */
    public String getExamName() {
        return examName;
    }

    /** 更新考试名称；调用方仍需遵守所属领域的校验规则。 */
    public void setExamName(String examName) {
        this.examName = examName;
    }

    /** 返回Full分数。 */
    public BigDecimal getFullScore() {
        return fullScore;
    }

    /** 更新Full分数；调用方仍需遵守所属领域的校验规则。 */
    public void setFullScore(BigDecimal fullScore) {
        this.fullScore = fullScore;
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

    /** 返回AnswersVisible。 */
    public Boolean getAnswersVisible() {
        return answersVisible;
    }

    /** 更新AnswersVisible；调用方仍需遵守所属领域的校验规则。 */
    public void setAnswersVisible(Boolean answersVisible) {
        this.answersVisible = answersVisible;
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
