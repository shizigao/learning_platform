/* 文件职责：实现学习资料分类业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.common.redis.RedisJsonCache;
import com.learningplatform.content.domain.ContentCategory;
import com.learningplatform.content.dto.CategoryWriteRequest;
import com.learningplatform.content.dto.ContentCategoryResponse;
import com.learningplatform.content.dto.ContentCategorySearchQuery;
import com.learningplatform.content.mapper.ContentCategoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.time.Duration;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
/**
 * 实现学习资料分类业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ContentCategoryService {
    private static final String ENABLED_CACHE_KEY = "lp:v1:content:categories:enabled";
    private static final TypeReference<List<ContentCategoryResponse>> CATEGORY_LIST_TYPE =
            new TypeReference<>() {
            };
    /** 访问分类持久化数据。 */
    private final ContentCategoryMapper categoryMapper;
    private final RedisJsonCache redisJsonCache;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ContentCategoryService(
            ContentCategoryMapper categoryMapper,
            RedisJsonCache redisJsonCache
    ) {
        this.categoryMapper = categoryMapper;
        this.redisJsonCache = redisJsonCache;
    }

    /** 查询启用状态相关数据；只返回当前调用方有权查看的结果。 */
    public List<ContentCategoryResponse> listEnabled() {
        return redisJsonCache.get(
                ENABLED_CACHE_KEY,
                CATEGORY_LIST_TYPE,
                Duration.ofMinutes(30),
                () -> categoryMapper.findAllEnabled().stream()
                        .map(ContentCategoryResponse::from)
                        .toList()
        );
    }

    /** 查询All相关数据；只返回当前调用方有权查看的结果。 */
    public List<ContentCategoryResponse> listAll() {
        return categoryMapper.findAll().stream()
                .map(ContentCategoryResponse::from)
                .toList();
    }

    /** 查询启用状态相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 返回Required。 */
    public ContentCategory getRequired(Long id) {
        return categoryMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在"));
    }

    /** 返回Required启用状态。 */
    public ContentCategory getRequiredEnabled(Long id) {
        ContentCategory category = getRequired(id);
        if (!Boolean.TRUE.equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "资料分类已停用");
        }
        return category;
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public ContentCategoryResponse create(CategoryWriteRequest request) {
        ContentCategory category = buildCategory(new ContentCategory(), request);
        validateParent(category.getParentId(), null);
        validateUniqueSlug(category.getSlug(), null);
        if (categoryMapper.insert(category) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建资料分类失败");
        }
        redisJsonCache.evictAfterCommit(ENABLED_CACHE_KEY);
        return ContentCategoryResponse.from(category);
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
    public ContentCategoryResponse update(Long id, CategoryWriteRequest request) {
        ContentCategory category = getRequired(id);
        buildCategory(category, request);
        validateParent(category.getParentId(), id);
        validateUniqueSlug(category.getSlug(), id);
        if (categoryMapper.update(category) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在");
        }
        redisJsonCache.evictAfterCommit(ENABLED_CACHE_KEY);
        return ContentCategoryResponse.from(getRequired(id));
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long id) {
        getRequired(id);
        if (categoryMapper.softDelete(id) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "资料分类不存在");
        }
        redisJsonCache.evictAfterCommit(ENABLED_CACHE_KEY);
    }

    /** 执行 buildCategory 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ContentCategory buildCategory(ContentCategory category, CategoryWriteRequest request) {
        category.setParentId(request.parentId());
        category.setName(request.name().trim());
        category.setSlug(request.slug().trim().toLowerCase(Locale.ROOT));
        category.setDescription(normalize(request.description()));
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setEnabled(request.enabled() == null || request.enabled());
        return category;
    }

    /** 校验Parent及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateParent(Long parentId, Long categoryId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(categoryId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分类不能将自身设为父分类");
        }
        getRequired(parentId);
    }

    /** 校验UniqueSlug及相关业务前置条件，不满足时抛出明确业务异常。 */
    private void validateUniqueSlug(String slug, Long excludedId) {
        if (categoryMapper.existsBySlug(slug, excludedId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "分类标识已存在");
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
