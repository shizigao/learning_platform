/* 文件职责：表示题目领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：题库、题目、选项与标准答案；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;

/**
 * 表示题目领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class Question extends BaseEntity {
    /** 保存题库ID，供该类型的业务逻辑读取或更新。 */
    private Long bankId;
    /** 保存creatorID，供该类型的业务逻辑读取或更新。 */
    private Long creatorId;
    /** 保存题目类型，供该类型的业务逻辑读取或更新。 */
    private QuestionType questionType;
    /** 保存stem，供该类型的业务逻辑读取或更新。 */
    private String stem;
    /** 保存答案Json，供该类型的业务逻辑读取或更新。 */
    private String answerJson;
    /** 保存答案Text，供该类型的业务逻辑读取或更新。 */
    private String answerText;
    /** 保存分析，供该类型的业务逻辑读取或更新。 */
    private String analysis;
    /** 保存default分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal defaultScore;
    /** 保存fillBlankAutoGradable，供该类型的业务逻辑读取或更新。 */
    private Boolean fillBlankAutoGradable;
    /** 保存case敏感配置，供该类型的业务逻辑读取或更新。 */
    private Boolean caseSensitive;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private QuestionStatus status;

    /** 返回题库ID。 */
    public Long getBankId() {
        return bankId;
    }

    /** 更新题库ID；调用方仍需遵守所属领域的校验规则。 */
    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    /** 返回CreatorID。 */
    public Long getCreatorId() {
        return creatorId;
    }

    /** 更新CreatorID；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
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

    /** 返回答案Json。 */
    public String getAnswerJson() {
        return answerJson;
    }

    /** 更新答案Json；调用方仍需遵守所属领域的校验规则。 */
    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    /** 返回答案Text。 */
    public String getAnswerText() {
        return answerText;
    }

    /** 更新答案Text；调用方仍需遵守所属领域的校验规则。 */
    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    /** 返回分析。 */
    public String getAnalysis() {
        return analysis;
    }

    /** 更新分析；调用方仍需遵守所属领域的校验规则。 */
    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }

    /** 返回Default分数。 */
    public BigDecimal getDefaultScore() {
        return defaultScore;
    }

    /** 更新Default分数；调用方仍需遵守所属领域的校验规则。 */
    public void setDefaultScore(BigDecimal defaultScore) {
        this.defaultScore = defaultScore;
    }

    /** 返回FillBlankAutoGradable。 */
    public Boolean getFillBlankAutoGradable() {
        return fillBlankAutoGradable;
    }

    /** 更新FillBlankAutoGradable；调用方仍需遵守所属领域的校验规则。 */
    public void setFillBlankAutoGradable(Boolean fillBlankAutoGradable) {
        this.fillBlankAutoGradable = fillBlankAutoGradable;
    }

    /** 返回Case敏感配置。 */
    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    /** 更新Case敏感配置；调用方仍需遵守所属领域的校验规则。 */
    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /** 返回状态。 */
    public QuestionStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
