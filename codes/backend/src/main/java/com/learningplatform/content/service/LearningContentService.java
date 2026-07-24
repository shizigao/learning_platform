package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.AdminContentListQuery;
import com.learningplatform.content.dto.ContentFileResponse;
import com.learningplatform.content.dto.ContentListQuery;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.dto.ContentWriteRequest;
import com.learningplatform.content.dto.PublisherContentListQuery;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearningContentService {
    private static final BigDecimal MINIMUM_PAID_PRICE = new BigDecimal("0.01");

    private final LearningContentMapper contentMapper;
    private final ContentFileMapper fileMapper;
    private final ContentCategoryService categoryService;
    private final ContentAccessService accessService;

    public LearningContentService(
            LearningContentMapper contentMapper,
            ContentFileMapper fileMapper,
            ContentCategoryService categoryService,
            ContentAccessService accessService
    ) {
        this.contentMapper = contentMapper;
        this.fileMapper = fileMapper;
        this.categoryService = categoryService;
        this.accessService = accessService;
    }

    @Transactional
    public ContentDetailResponse create(Long publisherId, ContentWriteRequest request) {
        categoryService.getRequiredEnabled(request.categoryId());
        LearningContent content = new LearningContent();
        content.setPublisherId(publisherId);
        applyWriteRequest(content, request);
        content.setStatus(ContentStatus.DRAFT);
        if (contentMapper.insert(content) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建学习资料失败");
        }
        return publisherDetail(content.getId(), publisherId, false);
    }

    @Transactional
    public ContentDetailResponse update(
            Long contentId,
            Long requesterUserId,
            boolean requesterAdmin,
            ContentWriteRequest request
    ) {
        categoryService.getRequiredEnabled(request.categoryId());
        LearningContent content = getRequired(contentId);
        assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        assertEditable(content);
        applyWriteRequest(content, request);
        if (content.getStatus() == ContentStatus.REJECTED) {
            content.setStatus(ContentStatus.DRAFT);
        }
        if (contentMapper.updateEditable(content) != 1) {
            throw invalidState("只有草稿或已驳回资料可以编辑");
        }
        return publisherDetail(contentId, requesterUserId, requesterAdmin);
    }

    public PageResult<ContentSummaryResponse> listPublished(ContentListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = contentMapper.countPublished(
                keyword,
                query.getCategoryId(),
                query.getContentType(),
                query.getFree()
        );
        List<ContentSummaryResponse> items = contentMapper.findPublished(
                        keyword,
                        query.getCategoryId(),
                        query.getContentType(),
                        query.getFree(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ContentSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    @Transactional
    public ContentDetailResponse publishedDetail(
            Long contentId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        contentMapper.incrementViewCount(contentId);
        content.setViewCount(valueOrZero(content.getViewCount()) + 1);
        boolean hasAccess = accessService.hasAccess(requesterUserId, requesterAdmin, content);
        return detail(content, hasAccess, hasAccess);
    }

    public ContentDetailResponse publishedDetail(Long contentId) {
        return publishedDetail(contentId, null, false);
    }

    public PageResult<ContentSummaryResponse> listByPublisher(
            Long publisherId,
            PublisherContentListQuery query
    ) {
        String keyword = normalize(query.getKeyword());
        long total = contentMapper.countByPublisher(publisherId, query.getStatus(), keyword);
        List<ContentSummaryResponse> items = contentMapper.findByPublisher(
                        publisherId,
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ContentSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public PageResult<ContentSummaryResponse> listForAdmin(AdminContentListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = contentMapper.countForAdmin(query.getStatus(), keyword);
        List<ContentSummaryResponse> items = contentMapper.findForAdmin(
                        query.getStatus(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(ContentSummaryResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public ContentDetailResponse publisherDetail(
            Long contentId,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        LearningContent content = getRequired(contentId);
        assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        return detail(content, true, true);
    }

    @Transactional
    public ContentDetailResponse submit(Long contentId, Long publisherId) {
        LearningContent content = getRequired(contentId);
        assertOwnerOrAdmin(content, publisherId, false);
        assertEditable(content);
        validateReadyForReview(content);
        if (contentMapper.submit(contentId, publisherId, LocalDateTime.now()) != 1) {
            throw invalidState("当前资料状态不能提交审核");
        }
        return publisherDetail(contentId, publisherId, false);
    }

    @Transactional
    public ContentDetailResponse approve(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PENDING_REVIEW
                || contentMapper.approve(contentId, LocalDateTime.now()) != 1) {
            throw invalidState("只有待审核资料可以审核通过");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    public ContentDetailResponse reject(Long contentId, String reason) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PENDING_REVIEW
                || contentMapper.reject(contentId, reason.trim()) != 1) {
            throw invalidState("只有待审核资料可以驳回");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    public ContentDetailResponse takeOffline(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PUBLISHED
                || contentMapper.takeOffline(contentId) != 1) {
            throw invalidState("只有已发布资料可以下架");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    public ContentDetailResponse republish(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.OFFLINE
                || contentMapper.republish(contentId, LocalDateTime.now()) != 1) {
            throw invalidState("只有已下架资料可以重新发布");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    public void delete(Long contentId, Long requesterUserId, boolean requesterAdmin) {
        LearningContent content = getRequired(contentId);
        assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        assertEditable(content);
        if (contentMapper.softDelete(contentId, content.getPublisherId()) != 1) {
            throw invalidState("只有草稿或已驳回资料可以删除");
        }
    }

    public LearningContent getRequired(Long contentId) {
        return contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
    }

    public void assertOwnerOrAdmin(
            LearningContent content,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        if (!requesterAdmin && !content.getPublisherId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的资料");
        }
    }

    public void assertEditable(LearningContent content) {
        if (content.getStatus() != ContentStatus.DRAFT
                && content.getStatus() != ContentStatus.REJECTED) {
            throw invalidState("当前资料状态不允许修改文件或内容");
        }
    }

    private void applyWriteRequest(LearningContent content, ContentWriteRequest request) {
        content.setCategoryId(request.categoryId());
        content.setTitle(request.title().trim());
        content.setSummary(normalize(request.summary()));
        content.setContentType(request.contentType());
        content.setArticleBody(normalize(request.articleBody()));
        content.setFree(request.isFree());
        content.setPrice(normalizePrice(request.isFree(), request.price()));
    }

    private BigDecimal normalizePrice(Boolean free, BigDecimal requestedPrice) {
        if (Boolean.TRUE.equals(free)) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (requestedPrice == null || requestedPrice.compareTo(MINIMUM_PAID_PRICE) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "付费资料价格不能低于0.01元");
        }
        return requestedPrice;
    }

    private void validateReadyForReview(LearningContent content) {
        boolean hasBody = content.getArticleBody() != null && !content.getArticleBody().isBlank();
        int relevantFiles = switch (content.getContentType()) {
            case ARTICLE -> 0;
            case DOCUMENT -> fileMapper.countByContentIdAndRole(content.getId(), ContentFileRole.CONTENT);
            case VIDEO -> fileMapper.countByContentIdAndRole(content.getId(), ContentFileRole.VIDEO);
            case ATTACHMENT -> fileMapper.countByContentIdAndRole(content.getId(), ContentFileRole.ATTACHMENT);
            case MIXED -> fileMapper.countByContentId(content.getId());
        };
        boolean ready = switch (content.getContentType()) {
            case ARTICLE -> hasBody;
            case DOCUMENT, VIDEO, ATTACHMENT -> relevantFiles > 0;
            case MIXED -> hasBody || relevantFiles > 0;
        };
        if (!ready) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "资料正文或对应文件尚未准备完整");
        }
    }

    private ContentDetailResponse detail(
            LearningContent content,
            boolean includeProtectedBody,
            boolean hasAccess
    ) {
        List<ContentFileResponse> files = fileMapper.findByContentId(content.getId()).stream()
                .map(ContentFileResponse::from)
                .toList();
        return ContentDetailResponse.from(content, files, includeProtectedBody, hasAccess);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
