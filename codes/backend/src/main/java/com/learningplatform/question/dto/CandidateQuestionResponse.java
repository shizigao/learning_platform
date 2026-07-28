/* 文件职责：定义考生题目响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：题库、题目、选项与标准答案；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.dto;

import com.learningplatform.question.domain.QuestionType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 考生侧安全投影：刻意不声明正确答案、解析以及选项正确性字段。
 */
public record CandidateQuestionResponse(
        Long id,
        QuestionType questionType,
        String stem,
        List<QuestionOptionResponse> options,
        BigDecimal score
) {
    public CandidateQuestionResponse {
        options = List.copyOf(options);
    }
}
