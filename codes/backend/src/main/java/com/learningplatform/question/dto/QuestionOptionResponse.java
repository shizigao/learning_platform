/* 文件职责：定义题目选项响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionOption;

/**
 * 定义题目选项响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record QuestionOptionResponse(
        Long id,
        String key,
        String text,
        int sortOrder
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(
                option.getId(),
                option.getOptionKey(),
                option.getOptionText(),
                option.getSortOrder()
        );
    }
}
