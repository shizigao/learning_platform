/* 文件职责：表示考试Statistics总结领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import java.math.BigDecimal;

/**
 * 表示考试Statistics总结领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamStatisticsSummary {
    /** 保存总计Candidates，供该类型的业务逻辑读取或更新。 */
    private Integer totalCandidates;
    /** 保存participated数量，供该类型的业务逻辑读取或更新。 */
    private Integer participatedCount;
    /** 保存提交数量，供该类型的业务逻辑读取或更新。 */
    private Integer submittedCount;
    /** 保存notParticipated数量，供该类型的业务逻辑读取或更新。 */
    private Integer notParticipatedCount;
    /** 保存graded数量，供该类型的业务逻辑读取或更新。 */
    private Integer gradedCount;
    /** 保存average分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal averageScore;
    /** 保存highest分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal highestScore;
    /** 保存lowest分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal lowestScore;
    /** 保存passed数量，供该类型的业务逻辑读取或更新。 */
    private Integer passedCount;

    /** 返回总计Candidates。 */
    public Integer getTotalCandidates() {
        return totalCandidates;
    }

    /** 更新总计Candidates；调用方仍需遵守所属领域的校验规则。 */
    public void setTotalCandidates(Integer totalCandidates) {
        this.totalCandidates = totalCandidates;
    }

    /** 返回Participated数量。 */
    public Integer getParticipatedCount() {
        return participatedCount;
    }

    /** 更新Participated数量；调用方仍需遵守所属领域的校验规则。 */
    public void setParticipatedCount(Integer participatedCount) {
        this.participatedCount = participatedCount;
    }

    /** 返回提交数量。 */
    public Integer getSubmittedCount() {
        return submittedCount;
    }

    /** 更新提交数量；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmittedCount(Integer submittedCount) {
        this.submittedCount = submittedCount;
    }

    /** 返回NotParticipated数量。 */
    public Integer getNotParticipatedCount() {
        return notParticipatedCount;
    }

    /** 更新NotParticipated数量；调用方仍需遵守所属领域的校验规则。 */
    public void setNotParticipatedCount(Integer notParticipatedCount) {
        this.notParticipatedCount = notParticipatedCount;
    }

    /** 返回Graded数量。 */
    public Integer getGradedCount() {
        return gradedCount;
    }

    /** 更新Graded数量；调用方仍需遵守所属领域的校验规则。 */
    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    /** 返回Average分数。 */
    public BigDecimal getAverageScore() {
        return averageScore;
    }

    /** 更新Average分数；调用方仍需遵守所属领域的校验规则。 */
    public void setAverageScore(BigDecimal averageScore) {
        this.averageScore = averageScore;
    }

    /** 返回Highest分数。 */
    public BigDecimal getHighestScore() {
        return highestScore;
    }

    /** 更新Highest分数；调用方仍需遵守所属领域的校验规则。 */
    public void setHighestScore(BigDecimal highestScore) {
        this.highestScore = highestScore;
    }

    /** 返回Lowest分数。 */
    public BigDecimal getLowestScore() {
        return lowestScore;
    }

    /** 更新Lowest分数；调用方仍需遵守所属领域的校验规则。 */
    public void setLowestScore(BigDecimal lowestScore) {
        this.lowestScore = lowestScore;
    }

    /** 返回Passed数量。 */
    public Integer getPassedCount() {
        return passedCount;
    }

    /** 更新Passed数量；调用方仍需遵守所属领域的校验规则。 */
    public void setPassedCount(Integer passedCount) {
        this.passedCount = passedCount;
    }
}
