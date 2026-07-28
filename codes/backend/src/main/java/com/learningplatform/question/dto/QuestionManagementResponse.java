/* 文件职责：定义题目Management响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.question.domain.Question;
import com.learningplatform.question.domain.QuestionStatus;
import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义题目Management响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record QuestionManagementResponse(
        Long id,
        Long bankId,
        Long creatorId,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        QuestionAnswer answer,
        String analysis,
        BigDecimal defaultScore,
        boolean fillBlankAutoGradable,
        boolean caseSensitive,
        QuestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public QuestionManagementResponse {
        options = List.copyOf(options);
    }
}
