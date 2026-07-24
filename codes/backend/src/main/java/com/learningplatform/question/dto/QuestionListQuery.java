package com.learningplatform.question.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.question.domain.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class QuestionListQuery extends PageQuery {
    @Min(value = 1, message = "题库ID必须为正数")
    private Long bankId;
    private QuestionType questionType;

    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
