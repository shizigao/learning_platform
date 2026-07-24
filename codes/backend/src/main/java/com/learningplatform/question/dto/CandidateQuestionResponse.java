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
