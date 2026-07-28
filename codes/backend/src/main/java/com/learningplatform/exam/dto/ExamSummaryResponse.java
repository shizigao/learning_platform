/* 文件职责：定义考试总结响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.Exam;
import com.learningplatform.exam.domain.ExamStatus;
import com.learningplatform.exam.domain.ExamAssignmentMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定义考试总结响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamSummaryResponse(
        Long id,
        Long publisherId,
        Long paperId,
        String name,
        ExamAssignmentMode assignmentMode,
        LocalDateTime startAt,
        LocalDateTime endAt,
        int durationMinutes,
        BigDecimal passingScore,
        boolean showResultImmediately,
        boolean showAnswerAfterFinish,
        ExamStatus status,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamSummaryResponse from(Exam exam) {
        return new ExamSummaryResponse(
                exam.getId(),
                exam.getPublisherId(),
                exam.getPaperId(),
                exam.getName(),
                exam.getAssignmentMode(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes(),
                exam.getPassingScore(),
                Boolean.TRUE.equals(exam.getShowResultImmediately()),
                Boolean.TRUE.equals(exam.getShowAnswerAfterFinish()),
                exam.getStatus(),
                exam.getPublishedAt(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}
