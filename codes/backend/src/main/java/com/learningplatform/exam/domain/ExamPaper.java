/* 文件职责：表示考试试卷领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

/**
 * 表示考试试卷领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamPaper extends BaseEntity {
    /** 保存creatorID，供该类型的业务逻辑读取或更新。 */
    private Long creatorId;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存总计分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal totalScore;
    /** 保存题目数量，供该类型的业务逻辑读取或更新。 */
    private Integer questionCount;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ExamPaperStatus status;

    /** 返回CreatorID。 */
    public Long getCreatorId() {
        return creatorId;
    }

    /** 更新CreatorID；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    /** 返回名称。 */
    public String getName() {
        return name;
    }

    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回Description。 */
    public String getDescription() {
        return description;
    }

    /** 更新Description；调用方仍需遵守所属领域的校验规则。 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 返回总计分数。 */
    public BigDecimal getTotalScore() {
        return totalScore;
    }

    /** 更新总计分数；调用方仍需遵守所属领域的校验规则。 */
    public void setTotalScore(BigDecimal totalScore) {
        this.totalScore = totalScore;
    }

    /** 返回题目数量。 */
    public Integer getQuestionCount() {
        return questionCount;
    }

    /** 更新题目数量；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    /** 返回状态。 */
    public ExamPaperStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ExamPaperStatus status) {
        this.status = status;
    }
}
