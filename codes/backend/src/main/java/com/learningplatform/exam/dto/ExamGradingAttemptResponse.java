/* 文件职责：定义考试阅卷作答响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamAttempt;
import com.learningplatform.exam.domain.ExamAttemptStatus;
import com.learningplatform.exam.domain.ExamSubmissionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定义考试阅卷作答响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamGradingAttemptResponse(
        Long attemptId,
        Long userId,
        String username,
        String nickname,
        ExamAttemptStatus status,
        LocalDateTime submittedAt,
        ExamSubmissionType submissionType,
        int pendingReviewCount,
        BigDecimal totalScore,
        boolean gradingCompleted
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamGradingAttemptResponse from(ExamAttempt attempt) {
        return new ExamGradingAttemptResponse(
                attempt.getId(),
                attempt.getUserId(),
                attempt.getUsername(),
                attempt.getNickname(),
                attempt.getStatus(),
                attempt.getSubmittedAt(),
                attempt.getSubmissionType(),
                attempt.getPendingReviewCount() == null ? 0 : attempt.getPendingReviewCount(),
                attempt.getFinalScore(),
                Boolean.TRUE.equals(attempt.getGradingCompleted())
        );
    }
}
