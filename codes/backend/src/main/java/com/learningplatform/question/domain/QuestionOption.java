/* 文件职责：表示题目选项领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：题库、题目、选项与标准答案；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示题目选项领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class QuestionOption extends BaseEntity {
    /** 保存题目ID，供该类型的业务逻辑读取或更新。 */
    private Long questionId;
    /** 保存选项键，供该类型的业务逻辑读取或更新。 */
    private String optionKey;
    /** 保存选项Text，供该类型的业务逻辑读取或更新。 */
    private String optionText;
    /** 保存correct，供该类型的业务逻辑读取或更新。 */
    private Boolean correct;
    /** 保存sort订单，供该类型的业务逻辑读取或更新。 */
    private Integer sortOrder;

    /** 返回题目ID。 */
    public Long getQuestionId() {
        return questionId;
    }

    /** 更新题目ID；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    /** 返回选项键。 */
    public String getOptionKey() {
        return optionKey;
    }

    /** 更新选项键；调用方仍需遵守所属领域的校验规则。 */
    public void setOptionKey(String optionKey) {
        this.optionKey = optionKey;
    }

    /** 返回选项Text。 */
    public String getOptionText() {
        return optionText;
    }

    /** 更新选项Text；调用方仍需遵守所属领域的校验规则。 */
    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    /** 返回Correct。 */
    public Boolean getCorrect() {
        return correct;
    }

    /** 更新Correct；调用方仍需遵守所属领域的校验规则。 */
    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    /** 返回Sort订单。 */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /** 更新Sort订单；调用方仍需遵守所属领域的校验规则。 */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
