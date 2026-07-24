package com.learningplatform.learning.dto;

import com.learningplatform.learning.domain.ContentComment;

import java.time.LocalDateTime;

public record ContentCommentResponse(
        Long id,
        Long contentId,
        Long userId,
        Long parentId,
        String body,
        LocalDateTime createdAt
) {
    public static ContentCommentResponse from(ContentComment comment) {
        return new ContentCommentResponse(
                comment.getId(),
                comment.getContentId(),
                comment.getUserId(),
                comment.getParentId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }
}
