package com.learningplatform.user.dto;

import com.learningplatform.content.domain.ContentPublicationStats;

public record UserPublicationStatsResponse(
        Long contentCount,
        Long viewCount,
        Long likeCount,
        Long favoriteCount
) {
    public static UserPublicationStatsResponse from(ContentPublicationStats stats) {
        return new UserPublicationStatsResponse(
                stats.getContentCount(),
                stats.getViewCount(),
                stats.getLikeCount(),
                stats.getFavoriteCount()
        );
    }
}
