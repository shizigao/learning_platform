/* 文件职责：表示考试答案领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示考试答案领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamAnswer extends BaseEntity {
    /** 保存作答ID，供该类型的业务逻辑读取或更新。 */
    private Long attemptId;
    /** 保存试卷题目ID，供该类型的业务逻辑读取或更新。 */
    private Long paperQuestionId;
    /** 保存题目ID，供该类型的业务逻辑读取或更新。 */
    private Long questionId;
    /** 保存答案Json，供该类型的业务逻辑读取或更新。 */
    private String answerJson;
    /** 保存答案Text，供该类型的业务逻辑读取或更新。 */
    private String answerText;
    /** 保存最大分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal maxScore;
    /** 保存分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal score;
    /** 保存correct，供该类型的业务逻辑读取或更新。 */
    private Boolean correct;
    /** 保存阅卷状态，供该类型的业务逻辑读取或更新。 */
    private ExamAnswerGradingStatus gradingStatus;
    /** 保存graderID，供该类型的业务逻辑读取或更新。 */
    private Long graderId;
    /** 保存grader评论，供该类型的业务逻辑读取或更新。 */
    private String graderComment;
    /** 保存saved时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime savedAt;
    /** 保存graded时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime gradedAt;
    /** 保存题目类型，供该类型的业务逻辑读取或更新。 */
    private QuestionType questionType;
    /** 保存optionsSnapshot，供该类型的业务逻辑读取或更新。 */
    private String optionsSnapshot;
    /** 保存答案Snapshot，供该类型的业务逻辑读取或更新。 */
    private String answerSnapshot;
    /** 保存分析Snapshot，供该类型的业务逻辑读取或更新。 */
    private String analysisSnapshot;
    /** 保存stemSnapshot，供该类型的业务逻辑读取或更新。 */
    private String stemSnapshot;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;
    /** 保存fillBlankAutoGradable，供该类型的业务逻辑读取或更新。 */
    private Boolean fillBlankAutoGradable;
    /** 保存case敏感配置，供该类型的业务逻辑读取或更新。 */
    private Boolean caseSensitive;

    /** 返回作答ID。 */
    public Long getAttemptId() {
        return attemptId;
    }

    /** 更新作答ID；调用方仍需遵守所属领域的校验规则。 */
    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    /** 返回试卷题目ID。 */
    public Long getPaperQuestionId() {
        return paperQuestionId;
    }

    /** 更新试卷题目ID；调用方仍需遵守所属领域的校验规则。 */
    public void setPaperQuestionId(Long paperQuestionId) {
        this.paperQuestionId = paperQuestionId;
    }

    /** 返回题目ID。 */
    public Long getQuestionId() {
        return questionId;
    }

    /** 更新题目ID；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
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

    /** 返回最大分数。 */
    public BigDecimal getMaxScore() {
        return maxScore;
    }

    /** 更新最大分数；调用方仍需遵守所属领域的校验规则。 */
    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    /** 返回分数。 */
    public BigDecimal getScore() {
        return score;
    }

    /** 更新分数；调用方仍需遵守所属领域的校验规则。 */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    /** 返回Correct。 */
    public Boolean getCorrect() {
        return correct;
    }

    /** 更新Correct；调用方仍需遵守所属领域的校验规则。 */
    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    /** 返回阅卷状态。 */
    public ExamAnswerGradingStatus getGradingStatus() {
        return gradingStatus;
    }

    /** 更新阅卷状态；调用方仍需遵守所属领域的校验规则。 */
    public void setGradingStatus(ExamAnswerGradingStatus gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    /** 返回GraderID。 */
    public Long getGraderId() {
        return graderId;
    }

    /** 更新GraderID；调用方仍需遵守所属领域的校验规则。 */
    public void setGraderId(Long graderId) {
        this.graderId = graderId;
    }

    /** 返回Grader评论。 */
    public String getGraderComment() {
        return graderComment;
    }

    /** 更新Grader评论；调用方仍需遵守所属领域的校验规则。 */
    public void setGraderComment(String graderComment) {
        this.graderComment = graderComment;
    }

    /** 返回Saved时间。 */
    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    /** 更新Saved时间；调用方仍需遵守所属领域的校验规则。 */
    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    /** 返回Graded时间。 */
    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    /** 更新Graded时间；调用方仍需遵守所属领域的校验规则。 */
    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    /** 返回题目类型。 */
    public QuestionType getQuestionType() {
        return questionType;
    }

    /** 更新题目类型；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    /** 返回OptionsSnapshot。 */
    public String getOptionsSnapshot() {
        return optionsSnapshot;
    }

    /** 更新OptionsSnapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setOptionsSnapshot(String optionsSnapshot) {
        this.optionsSnapshot = optionsSnapshot;
    }

    /** 返回答案Snapshot。 */
    public String getAnswerSnapshot() {
        return answerSnapshot;
    }

    /** 更新答案Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setAnswerSnapshot(String answerSnapshot) {
        this.answerSnapshot = answerSnapshot;
    }

    /** 返回分析Snapshot。 */
    public String getAnalysisSnapshot() {
        return analysisSnapshot;
    }

    /** 更新分析Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setAnalysisSnapshot(String analysisSnapshot) {
        this.analysisSnapshot = analysisSnapshot;
    }

    /** 返回StemSnapshot。 */
    public String getStemSnapshot() {
        return stemSnapshot;
    }

    /** 更新StemSnapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setStemSnapshot(String stemSnapshot) {
        this.stemSnapshot = stemSnapshot;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
}
