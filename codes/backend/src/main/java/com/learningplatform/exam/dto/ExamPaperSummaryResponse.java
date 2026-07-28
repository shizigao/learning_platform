/* 文件职责：定义考试试卷总结响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamPaper;
import com.learningplatform.exam.domain.ExamPaperStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定义考试试卷总结响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamPaperSummaryResponse(
        Long id,
        Long creatorId,
        String name,
        String description,
        BigDecimal totalScore,
        int questionCount,
        ExamPaperStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamPaperSummaryResponse from(ExamPaper paper) {
        return new ExamPaperSummaryResponse(
                paper.getId(),
                paper.getCreatorId(),
                paper.getName(),
                paper.getDescription(),
                paper.getTotalScore(),
                paper.getQuestionCount(),
                paper.getStatus(),
                paper.getCreatedAt(),
                paper.getUpdatedAt()
        );
    }
}
