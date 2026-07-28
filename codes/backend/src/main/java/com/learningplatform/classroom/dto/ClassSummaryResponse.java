/* 文件职责：定义班级总结响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.dto;

import com.learningplatform.classroom.domain.ClassRole;
import com.learningplatform.classroom.domain.ClassStatus;

import java.time.LocalDateTime;

/**
 * 定义班级总结响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ClassSummaryResponse(
        Long id,
        Long ownerId,
        String name,
        String description,
        ClassStatus status,
        ClassRole currentRole,
        long memberCount,
        String inviteCode,
        Boolean inviteEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
