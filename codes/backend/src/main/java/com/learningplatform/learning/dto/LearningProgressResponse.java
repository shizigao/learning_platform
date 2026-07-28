/* 文件职责：定义学习进度响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.dto;

import com.learningplatform.learning.domain.LearningProgress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 定义学习进度响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record LearningProgressResponse(
        Long id,
        Long contentId,
        LocalDateTime startedAt,
        LocalDateTime lastLearnedAt,
        BigDecimal progressPercent,
        String lastPosition,
        LocalDateTime completedAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static LearningProgressResponse from(LearningProgress progress) {
        return new LearningProgressResponse(
                progress.getId(),
                progress.getContentId(),
                progress.getStartedAt(),
                progress.getLastLearnedAt(),
                progress.getProgressPercent(),
                progress.getLastPosition(),
                progress.getCompletedAt()
        );
    }
}
