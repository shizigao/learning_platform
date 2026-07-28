/* 文件职责：定义错题复习考试响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义错题复习考试响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record WrongReviewExamResponse(
        Long resultId,
        Long examId,
        String examName,
        BigDecimal fullScore,
        BigDecimal totalScore,
        BigDecimal passingScore,
        boolean passed,
        boolean answersVisible,
        LocalDateTime generatedAt,
        List<ExamResultQuestionResponse> questions
) {
    public WrongReviewExamResponse {
        questions = List.copyOf(questions);
    }
}
