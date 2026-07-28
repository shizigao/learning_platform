/* 文件职责：定义题目列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.question.domain.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 定义题目列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class QuestionListQuery extends PageQuery {
    @Min(value = 1, message = "题库ID必须为正数")
    /** 保存题库ID，供该类型的业务逻辑读取或更新。 */
    private Long bankId;
    /** 保存题目类型，供该类型的业务逻辑读取或更新。 */
    private QuestionType questionType;

    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    /** 保存keyword，供该类型的业务逻辑读取或更新。 */
    private String keyword;

    /** 返回题库ID。 */
    public Long getBankId() {
        return bankId;
    }

    /** 更新题库ID；调用方仍需遵守所属领域的校验规则。 */
    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    /** 返回题目类型。 */
    public QuestionType getQuestionType() {
        return questionType;
    }

    /** 更新题目类型；调用方仍需遵守所属领域的校验规则。 */
    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    /** 返回Keyword。 */
    public String getKeyword() {
        return keyword;
    }

    /** 更新Keyword；调用方仍需遵守所属领域的校验规则。 */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
