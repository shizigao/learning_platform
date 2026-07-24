package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExamAnswer extends BaseEntity {
    private Long attemptId;
    private Long paperQuestionId;
    private Long questionId;
    private String answerJson;
    private String answerText;
    private BigDecimal maxScore;
    private BigDecimal score;
    private Boolean correct;
    private ExamAnswerGradingStatus gradingStatus;
    private Long graderId;
    private String graderComment;
    private LocalDateTime savedAt;
    private LocalDateTime gradedAt;
    private QuestionType questionType;
    private String optionsSnapshot;
    private String answerSnapshot;
    private String analysisSnapshot;
    private String stemSnapshot;
    private Integer sortOrder;
    private Boolean fillBlankAutoGradable;
    private Boolean caseSensitive;

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getPaperQuestionId() {
        return paperQuestionId;
    }

    public void setPaperQuestionId(Long paperQuestionId) {
        this.paperQuestionId = paperQuestionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getAnswerJson() {
        return answerJson;
    }

    public void setAnswerJson(String answerJson) {
        this.answerJson = answerJson;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public ExamAnswerGradingStatus getGradingStatus() {
        return gradingStatus;
    }

    public void setGradingStatus(ExamAnswerGradingStatus gradingStatus) {
        this.gradingStatus = gradingStatus;
    }

    public Long getGraderId() {
        return graderId;
    }

    public void setGraderId(Long graderId) {
        this.graderId = graderId;
    }

    public String getGraderComment() {
        return graderComment;
    }

    public void setGraderComment(String graderComment) {
        this.graderComment = graderComment;
    }

    public LocalDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(LocalDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public LocalDateTime getGradedAt() {
        return gradedAt;
    }

    public void setGradedAt(LocalDateTime gradedAt) {
        this.gradedAt = gradedAt;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getOptionsSnapshot() {
        return optionsSnapshot;
    }

    public void setOptionsSnapshot(String optionsSnapshot) {
        this.optionsSnapshot = optionsSnapshot;
    }

    public String getAnswerSnapshot() {
        return answerSnapshot;
    }

    public void setAnswerSnapshot(String answerSnapshot) {
        this.answerSnapshot = answerSnapshot;
    }

    public String getAnalysisSnapshot() {
        return analysisSnapshot;
    }

    public void setAnalysisSnapshot(String analysisSnapshot) {
        this.analysisSnapshot = analysisSnapshot;
    }

    public String getStemSnapshot() {
        return stemSnapshot;
    }

    public void setStemSnapshot(String stemSnapshot) {
        this.stemSnapshot = stemSnapshot;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getFillBlankAutoGradable() {
        return fillBlankAutoGradable;
    }

    public void setFillBlankAutoGradable(Boolean fillBlankAutoGradable) {
        this.fillBlankAutoGradable = fillBlankAutoGradable;
    }

    public Boolean getCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(Boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
}
