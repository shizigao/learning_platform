/* 文件职责：定义考试考生响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.dto;

import com.learningplatform.exam.domain.ExamCandidate;
import com.learningplatform.exam.domain.ExamCandidateStatus;

import java.time.LocalDateTime;

/**
 * 定义考试考生响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ExamCandidateResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        ExamCandidateStatus status,
        LocalDateTime assignedAt,
        LocalDateTime startedAt,
        LocalDateTime submittedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ExamCandidateResponse from(ExamCandidate candidate) {
        return new ExamCandidateResponse(
                candidate.getId(),
                candidate.getUserId(),
                candidate.getUsername(),
                candidate.getNickname(),
                candidate.getStatus(),
                candidate.getAssignedAt(),
                candidate.getStartedAt(),
                candidate.getSubmittedAt()
        );
    }
}
