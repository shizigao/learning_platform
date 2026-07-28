/* 文件职责：定义学习资料分类响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.dto;

import com.learningplatform.content.domain.ContentCategory;

/**
 * 定义学习资料分类响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record ContentCategoryResponse(
        Long id,
        Long parentId,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean enabled
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
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
