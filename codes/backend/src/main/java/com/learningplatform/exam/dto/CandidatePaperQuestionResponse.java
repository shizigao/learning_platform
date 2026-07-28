/* 文件职责：定义考生试卷题目响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.question.domain.QuestionType;
import com.learningplatform.question.dto.QuestionOptionResponse;

import java.math.BigDecimal;
import java.util.List;

/**
 * 定义考生试卷题目响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record CandidatePaperQuestionResponse(
        Long paperQuestionId,
        Long questionId,
        int sortOrder,
        BigDecimal score,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        int blankCount
) {
    public CandidatePaperQuestionResponse {
        options = List.copyOf(options);
    }
}
