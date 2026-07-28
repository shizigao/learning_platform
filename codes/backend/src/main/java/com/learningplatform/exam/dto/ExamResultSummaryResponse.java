/* 文件职责：定义考试成绩总结响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamResult;
import com.learningplatform.exam.domain.ExamAnswerGradingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义考试成绩总结响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamResultSummaryResponse(
        Long id,
        Long examId,
        Long attemptId,
        Long userId,
        BigDecimal totalScore,
        BigDecimal passingScore,
        boolean passed,
        int correctCount,
        int partialCreditCount,
        int incorrectCount,
        int unansweredCount,
        int pendingReviewCount,
        boolean gradingCompleted,
        LocalDateTime generatedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamResultSummaryResponse from(ExamResult result) {
        return new ExamResultSummaryResponse(
                result.getId(),
                result.getExamId(),
                result.getAttemptId(),
                result.getUserId(),
                result.getTotalScore(),
                result.getPassingScore(),
                Boolean.TRUE.equals(result.getPassed()),
                result.getCorrectCount(),
                0,
                result.getIncorrectCount(),
                result.getUnansweredCount(),
                0,
                Boolean.TRUE.equals(result.getGradingCompleted()),
                result.getGeneratedAt()
        );
    }

    public static ExamResultSummaryResponse from(
            ExamResult result,
            List<ExamResultQuestionResponse> questions
    ) {
        int correctCount = (int) questions.stream()
                .filter(question -> Boolean.TRUE.equals(question.correct()))
                .count();
        int partialCreditCount = (int) questions.stream()
                .filter(question -> question.score() != null)
                .filter(question -> question.score().compareTo(BigDecimal.ZERO) > 0)
                .filter(question -> question.score().compareTo(question.maxScore()) < 0)
                .count();
        int incorrectCount = (int) questions.stream()
                .filter(question -> question.score() != null)
                .filter(question -> question.score().compareTo(BigDecimal.ZERO) == 0)
                .filter(question -> question.gradingStatus() != ExamAnswerGradingStatus.UNANSWERED)
                .filter(question -> question.gradingStatus() != ExamAnswerGradingStatus.PENDING_REVIEW)
                .count();
        int unansweredCount = (int) questions.stream()
                .filter(question -> question.gradingStatus() == ExamAnswerGradingStatus.UNANSWERED)
                .count();
        int pendingReviewCount = (int) questions.stream()
                .filter(question -> question.gradingStatus() == ExamAnswerGradingStatus.PENDING_REVIEW)
                .count();
        return new ExamResultSummaryResponse(
                result.getId(),
                result.getExamId(),
                result.getAttemptId(),
                result.getUserId(),
                result.getTotalScore(),
                result.getPassingScore(),
                Boolean.TRUE.equals(result.getPassed()),
                correctCount,
                partialCreditCount,
                incorrectCount,
                unansweredCount,
                pendingReviewCount,
                Boolean.TRUE.equals(result.getGradingCompleted()),
                result.getGeneratedAt()
        );
    }
}
