/* 文件职责：表示考试题目Statistics领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;

/**
 * 表示考试题目Statistics领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamQuestionStatistics {
    /** 保存题目ID，供该类型的业务逻辑读取或更新。 */
    private Long questionId;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;
    /** 保存题目类型，供该类型的业务逻辑读取或更新。 */
    private QuestionType questionType;
    /** 保存stem，供该类型的业务逻辑读取或更新。 */
    private String stem;
    /** 保存最大分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal maxScore;
    /** 保存graded数量，供该类型的业务逻辑读取或更新。 */
    private Integer gradedCount;
    /** 保存answered数量，供该类型的业务逻辑读取或更新。 */
    private Integer answeredCount;
    /** 保存correct数量，供该类型的业务逻辑读取或更新。 */
    private Integer correctCount;

    /** 返回题目ID。 */
    public Long getQuestionId() {
        return questionId;
    }

    /** 更新题目ID；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /** 返回题目类型。 */
    public QuestionType getQuestionType() {
        return questionType;
    }

    /** 更新题目类型；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    /** 返回Stem。 */
    public String getStem() {
        return stem;
    }

    /** 更新Stem；调用方仍需遵守所属领域的校验规则。 */
    public void setStem(String stem) {
        this.stem = stem;
    }

    /** 返回最大分数。 */
    public BigDecimal getMaxScore() {
        return maxScore;
    }

    /** 更新最大分数；调用方仍需遵守所属领域的校验规则。 */
    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    /** 返回Graded数量。 */
    public Integer getGradedCount() {
        return gradedCount;
    }

    /** 更新Graded数量；调用方仍需遵守所属领域的校验规则。 */
    public void setGradedCount(Integer gradedCount) {
        this.gradedCount = gradedCount;
    }

    /** 返回Answered数量。 */
    public Integer getAnsweredCount() {
        return answeredCount;
    }

    /** 更新Answered数量；调用方仍需遵守所属领域的校验规则。 */
    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    /** 返回Correct数量。 */
    public Integer getCorrectCount() {
        return correctCount;
    }

    /** 更新Correct数量；调用方仍需遵守所属领域的校验规则。 */
    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }
}
