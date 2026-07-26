package com.learningplatform.content.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.classroom.mapper.ClassScopeMapper;
import com.learningplatform.classroom.service.ClassroomService;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentPublicationStats;
import com.learningplatform.content.domain.ContentType;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.AdminContentListQuery;
import com.learningplatform.content.dto.ContentFileResponse;
import com.learningplatform.content.dto.ContentListQuery;
import com.learningplatform.content.dto.ContentReferenceSearchQuery;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.dto.ContentWriteRequest;
import com.learningplatform.content.dto.PublisherContentListQuery;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.storage.MinioStorageService;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.service.UserAvatarService;
import com.learningplatform.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearningContentService {
    private static final Logger log = LoggerFactory.getLogger(LearningContentService.class);
    private static final BigDecimal MINIMUM_PAID_PRICE = new BigDecimal("0.01");

    private final LearningContentMapper contentMapper;
    private final ContentFileMapper fileMapper;
    private final ContentCategoryService categoryService;
    private final ContentAccessService accessService;
    private final MinioStorageService storageService;
    private final ClassScopeMapper classScopeMapper;
    private final ClassroomService classroomService;
    private final UserService userService;
    private final UserAvatarService avatarService;

    public LearningContentService(
            LearningContentMapper contentMapper,
            ContentFileMapper fileMapper,
            ContentCategoryService categoryService,
            ContentAccessService accessService,
            MinioStorageService storageService,
            ClassScopeMapper classScopeMapper,
            ClassroomService classroomService,
            UserService userService,
            UserAvatarService avatarService
    ) {
        this.contentMapper = contentMapper;
        this.fileMapper = fileMapper;
        this.categoryService = categoryService;
        this.accessService = accessService;
        this.storageService = storageService;
        this.classScopeMapper = classScopeMapper;
        this.classroomService = classroomService;
        this.userService = userService;
        this.avatarService = avatarService;
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
        replaceClassScopes(content.getId(), publisherId, request);
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
        replaceClassScopes(contentId, requesterUserId, request);
        return publisherDetail(contentId, requesterUserId, requesterAdmin);
    }

    public PageResult<ContentSummaryResponse> listPublished(ContentListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = contentMapper.countPublished(
                keyword,
                query.getCategoryId(),
                null,
                query.getFree()
        );
        List<ContentSummaryResponse> items = contentMapper.findPublished(
                        keyword,
                        query.getCategoryId(),
                        null,
                        query.getFree(),
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::summary)
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
        if (content.getDistributionMode() == ContentDistributionMode.CLASS && !hasAccess) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅资料发放班级的有效成员可以访问");
        }
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
                .map(this::summary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public PageResult<ContentSummaryResponse> listPublicByPublisher(
            Long publisherId,
            PageQuery query
    ) {
        long total = contentMapper.countPublicByPublisher(publisherId);
        List<ContentSummaryResponse> items = contentMapper.findPublicByPublisher(
                        publisherId,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::summary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public ContentPublicationStats publicationStats(Long publisherId) {
        return contentMapper.publicationStats(publisherId);
    }

    public PageResult<ContentSummaryResponse> listForClass(
            Long classId,
            Long requesterId,
            PageQuery query
    ) {
        classroomService.requireActiveMember(classId, requesterId);
        long total = classScopeMapper.countPublishedContents(classId);
        List<ContentSummaryResponse> items = classScopeMapper.findPublishedContents(
                        classId,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::summary)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public PageResult<ContentSummaryResponse> listReferenceCandidates(
            ContentReferenceSearchQuery query
    ) {
        String titleKeyword = normalize(query.getTitleKeyword());
        String publisherKeyword = normalize(query.getPublisherKeyword());
        long total = contentMapper.countReferenceCandidates(
                titleKeyword,
                publisherKeyword,
                query.getExcludeContentId()
        );
        List<ContentSummaryResponse> items = contentMapper.findReferenceCandidates(
                        titleKeyword,
                        publisherKeyword,
                        query.getExcludeContentId(),
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
                .map(this::summary)
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
        content.setContentType(ContentType.GENERAL);
        content.setArticleBody(normalize(request.articleBody()));
        ContentDistributionMode mode = request.distributionMode() == null
                ? ContentDistributionMode.PUBLIC
                : request.distributionMode();
        content.setDistributionMode(mode);
        if (mode == ContentDistributionMode.CLASS) {
            content.setFree(true);
            content.setPrice(BigDecimal.ZERO.setScale(2));
        } else {
            content.setFree(request.isFree());
            content.setPrice(normalizePrice(request.isFree(), request.price()));
        }
    }

    private void replaceClassScopes(
            Long contentId,
            Long publisherId,
            ContentWriteRequest request
    ) {
        classScopeMapper.deleteContentScopes(contentId);
        ContentDistributionMode mode = request.distributionMode() == null
                ? ContentDistributionMode.PUBLIC
                : request.distributionMode();
        if (mode != ContentDistributionMode.CLASS) return;
        classroomService.requireManageableClasses(request.classIds(), publisherId);
        request.classIds().forEach(classId -> {
            if (classScopeMapper.insertContentScope(contentId, classId) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存资料班级范围失败");
            }
        });
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
        int relevantFiles = fileMapper.countByContentId(content.getId())
                - fileMapper.countByContentIdAndRole(content.getId(), ContentFileRole.COVER)
                - fileMapper.countByContentIdAndRole(content.getId(), ContentFileRole.INLINE_IMAGE);
        boolean ready = hasBody || relevantFiles > 0;
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
        User publisher = userService.getRequiredById(content.getPublisherId());
        return ContentDetailResponse.from(
                content,
                files,
                includeProtectedBody,
                hasAccess,
                coverUrl(content),
                classScopeMapper.findContentClassIds(content.getId()),
                publisher.getUsername(),
                avatarService.avatarUrl(publisher)
        );
    }

    public ContentSummaryResponse summary(LearningContent content) {
        return ContentSummaryResponse.from(content, coverUrl(content));
    }

    private String coverUrl(LearningContent content) {
        if (content.getCoverFileId() == null) {
            return null;
        }
        return fileMapper.findById(content.getCoverFileId())
                .filter(file -> file.getFileRole() == ContentFileRole.COVER)
                .map(file -> {
                    try {
                        return storageService.createAuthorizedPreviewUrl(file.getObjectName());
                    } catch (RuntimeException exception) {
                        log.warn(
                                "Unable to create cover URL for contentId={} fileId={}",
                                content.getId(),
                                file.getId()
                        );
                        return null;
                    }
                })
                .orElse(null);
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
