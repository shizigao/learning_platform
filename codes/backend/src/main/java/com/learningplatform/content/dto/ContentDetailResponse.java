package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.LearningContent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
