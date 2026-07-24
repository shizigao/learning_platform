package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;

public class ExamPaperQuestion extends BaseEntity {
    private Long paperId;
    private Long questionId;
    private Integer sortOrder;
    private BigDecimal score;
    private QuestionType questionTypeSnapshot;
    private String stemSnapshot;
    private String optionsSnapshot;
    private String answerSnapshot;
    private String analysisSnapshot;

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public QuestionType getQuestionTypeSnapshot() {
        return questionTypeSnapshot;
    }

    public void setQuestionTypeSnapshot(QuestionType questionTypeSnapshot) {
        this.questionTypeSnapshot = questionTypeSnapshot;
    }

    public String getStemSnapshot() {
        return stemSnapshot;
    }

    public void setStemSnapshot(String stemSnapshot) {
        this.stemSnapshot = stemSnapshot;
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
}
