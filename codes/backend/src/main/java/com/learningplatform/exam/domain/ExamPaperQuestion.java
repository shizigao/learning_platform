/* 文件职责：表示考试试卷题目领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;

/**
 * 表示考试试卷题目领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamPaperQuestion extends BaseEntity {
    /** 保存试卷ID，供该类型的业务逻辑读取或更新。 */
    private Long paperId;
    /** 保存题目ID，供该类型的业务逻辑读取或更新。 */
    private Long questionId;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;
    /** 保存分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal score;
    /** 保存题目类型Snapshot，供该类型的业务逻辑读取或更新。 */
    private QuestionType questionTypeSnapshot;
    /** 保存stemSnapshot，供该类型的业务逻辑读取或更新。 */
    private String stemSnapshot;
    /** 保存optionsSnapshot，供该类型的业务逻辑读取或更新。 */
    private String optionsSnapshot;
    /** 保存答案Snapshot，供该类型的业务逻辑读取或更新。 */
    private String answerSnapshot;
    /** 保存分析Snapshot，供该类型的业务逻辑读取或更新。 */
    private String analysisSnapshot;

    /** 返回试卷ID。 */
    public Long getPaperId() {
        return paperId;
    }

    /** 更新试卷ID；调用方仍需遵守所属领域的校验规则。 */
    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

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

    /** 返回分数。 */
    public BigDecimal getScore() {
        return score;
    }

    /** 更新分数；调用方仍需遵守所属领域的校验规则。 */
    public void setScore(BigDecimal score) {
        this.score = score;
    }

    /** 返回题目类型Snapshot。 */
    public QuestionType getQuestionTypeSnapshot() {
        return questionTypeSnapshot;
    }

    /** 更新题目类型Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionTypeSnapshot(QuestionType questionTypeSnapshot) {
        this.questionTypeSnapshot = questionTypeSnapshot;
    }

    /** 返回StemSnapshot。 */
    public String getStemSnapshot() {
        return stemSnapshot;
    }

    /** 更新StemSnapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setStemSnapshot(String stemSnapshot) {
        this.stemSnapshot = stemSnapshot;
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
}
