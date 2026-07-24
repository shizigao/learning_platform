package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.LearningContent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ContentSummaryResponse(
        Long id,
        Long publisherId,
        String publisherName,
        Long categoryId,
        String title,
        String summary,
        ContentType contentType,
        Long coverFileId,
        Boolean isFree,
        BigDecimal price,
        ContentStatus status,
        Long viewCount,
        Long likeCount,
        Long favoriteCount,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public static ContentSummaryResponse from(LearningContent content) {
        return new ContentSummaryResponse(
                content.getId(),
                content.getPublisherId(),
                content.getPublisherName(),
                content.getCategoryId(),
                content.getTitle(),
                content.getSummary(),
                content.getContentType(),
                content.getCoverFileId(),
                content.getFree(),
                content.getPrice(),
                content.getStatus(),
                content.getViewCount(),
                content.getLikeCount(),
                content.getFavoriteCount(),
                content.getPublishedAt(),
                content.getUpdatedAt()
        );
    }
}
