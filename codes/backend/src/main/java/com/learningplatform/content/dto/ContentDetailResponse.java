/* 文件职责：定义学习资料详情响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.LearningContent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定义学习资料详情响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ContentDetailResponse(
        Long id,
        Long publisherId,
        String publisherName,
        String publisherUsername,
        String publisherAvatarUrl,
        Long categoryId,
        String categoryName,
        String title,
        String summary,
        ContentType contentType,
        String articleBody,
        Long coverFileId,
        String coverUrl,
        ContentDistributionMode distributionMode,
        List<Long> classIds,
        Boolean isFree,
        BigDecimal price,
        Boolean hasAccess,
        ContentStatus status,
        String rejectionReason,
        Long viewCount,
        Long likeCount,
        Long favoriteCount,
        Long commentCount,
        LocalDateTime submittedAt,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ContentFileResponse> files
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static ContentDetailResponse from(
            LearningContent content,
            List<ContentFileResponse> files,
            boolean includeProtectedBody,
            boolean hasAccess,
            String coverUrl,
            List<Long> classIds,
            String publisherUsername,
            String publisherAvatarUrl
    ) {
        return new ContentDetailResponse(
                content.getId(),
                content.getPublisherId(),
                content.getPublisherName(),
                publisherUsername,
                publisherAvatarUrl,
                content.getCategoryId(),
                content.getCategoryName(),
                content.getTitle(),
                content.getSummary(),
                content.getContentType(),
                includeProtectedBody ? content.getArticleBody() : null,
                content.getCoverFileId(),
                coverUrl,
                content.getDistributionMode(),
                List.copyOf(classIds),
                content.getFree(),
                content.getPrice(),
                hasAccess,
                content.getStatus(),
                content.getRejectionReason(),
                content.getViewCount(),
                content.getLikeCount(),
                content.getFavoriteCount(),
                content.getCommentCount(),
                content.getSubmittedAt(),
                content.getPublishedAt(),
                content.getCreatedAt(),
                content.getUpdatedAt(),
                List.copyOf(files)
        );
    }
}
