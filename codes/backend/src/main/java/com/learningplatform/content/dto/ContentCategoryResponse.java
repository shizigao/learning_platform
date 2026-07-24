package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentCategory;

public record ContentCategoryResponse(
        Long id,
        Long parentId,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean enabled
) {
    public static ContentCategoryResponse from(ContentCategory category) {
        return new ContentCategoryResponse(
                category.getId(),
                category.getParentId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getSortOrder(),
                category.getEnabled()
        );
    }
}
