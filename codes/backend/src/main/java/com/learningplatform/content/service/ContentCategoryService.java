package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.domain.ContentCategory;
import com.learningplatform.content.dto.CategoryWriteRequest;
import com.learningplatform.content.dto.ContentCategoryResponse;
import com.learningplatform.content.dto.ContentCategorySearchQuery;
import com.learningplatform.content.mapper.ContentCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class ContentCategoryService {
    private final ContentCategoryMapper categoryMapper;

    public ContentCategoryService(ContentCategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<ContentCategoryResponse> listEnabled() {
        return categoryMapper.findAllEnabled().stream()
                .map(ContentCategoryResponse::from)
                .toList();
    }

    public List<ContentCategoryResponse> listAll() {
        return categoryMapper.findAll().stream()
                .map(ContentCategoryResponse::from)
                .toList();
    }

    public PageResult<ContentCategoryResponse> searchEnabled(ContentCategorySearchQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = categoryMapper.countEnabled(keyword);
        List<ContentCategoryResponse> items = categoryMapper.searchEnabled(
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ContentCategoryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public ContentCategory getRequired(Long id) {
        return categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在"));
    }

    public ContentCategory getRequiredEnabled(Long id) {
        ContentCategory category = getRequired(id);
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "资料分类已停用");
        }
        return category;
    }

    @Transactional
    public ContentCategoryResponse create(CategoryWriteRequest request) {
        ContentCategory category = buildCategory(new ContentCategory(), request);
        validateParent(category.getParentId(), null);
        validateUniqueSlug(category.getSlug(), null);
        if (categoryMapper.insert(category) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建资料分类失败");
        }
        return ContentCategoryResponse.from(category);
    }

    @Transactional
    public ContentCategoryResponse update(Long id, CategoryWriteRequest request) {
        ContentCategory category = getRequired(id);
        buildCategory(category, request);
        validateParent(category.getParentId(), id);
        validateUniqueSlug(category.getSlug(), id);
        if (categoryMapper.update(category) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在");
        }
        return ContentCategoryResponse.from(getRequired(id));
    }

    @Transactional
    public void delete(Long id) {
        getRequired(id);
        if (categoryMapper.softDelete(id) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在");
        }
    }

    private ContentCategory buildCategory(ContentCategory category, CategoryWriteRequest request) {
        category.setParentId(request.parentId());
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim().toLowerCase(Locale.ROOT));
        category.setDescription(normalize(request.description()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(request.enabled() == null || request.enabled());
        return category;
    }

    private void validateParent(Long parentId, Long categoryId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类不能将自身设为父分类");
        }
        getRequired(parentId);
    }

    private void validateUniqueSlug(String slug, Long excludedId) {
        if (categoryMapper.existsBySlug(slug, excludedId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类标识已存在");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
