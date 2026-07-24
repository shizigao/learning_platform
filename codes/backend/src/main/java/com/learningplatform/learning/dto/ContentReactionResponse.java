package com.learningplatform.learning.dto;

public record ContentReactionResponse(
        boolean liked,
        boolean favorited,
        long likeCount,
        long favoriteCount
) {
}
