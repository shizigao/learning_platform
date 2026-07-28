/* 文件职责：实现学习资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现学习资料业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class LearningContentService {
    private static final Logger log = LoggerFactory.getLogger(LearningContentService.class);
    private static final BigDecimal MINIMUM_PAID_PRICE = new BigDecimal("0.01");

    /** 访问学习资料持久化数据。 */
    private final LearningContentMapper contentMapper;
    /** 访问文件持久化数据。 */
    private final ContentFileMapper fileMapper;
    /** 委托分类执行对应领域规则。 */
    private final ContentCategoryService categoryService;
    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;
    /** 委托存储执行对应领域规则。 */
    private final MinioStorageService storageService;
    /** 访问班级范围持久化数据。 */
    private final ClassScopeMapper classScopeMapper;
    /** 委托班级执行对应领域规则。 */
    private final ClassroomService classroomService;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
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
        //点击applyWriteRequest
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

    /** 查询发布相关数据；只返回当前调用方有权查看的结果。 */
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
    /** 执行发布状态流转，仅允许从合法前置状态进入目标状态。 */
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

    /** 执行发布状态流转，仅允许从合法前置状态进入目标状态。 */
    public ContentDetailResponse publishedDetail(Long contentId) {
        return publishedDetail(contentId, null, false);
    }

    /** 按发布者查询数据；只返回当前调用方有权查看的结果。 */
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

    /** 查询Public按发布者相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 执行 publicationStats 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentPublicationStats publicationStats(Long publisherId) {
        return contentMapper.publicationStats(publisherId);
    }

    /** 查询For班级相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 查询ReferenceCandidates相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 查询For管理相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 执行发布状态流转，仅允许从合法前置状态进入目标状态。 */
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
    /** 执行提交状态流转，仅允许从合法前置状态进入目标状态。 */
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
    /** 执行审核通过状态流转，仅允许从合法前置状态进入目标状态。 */
    public ContentDetailResponse approve(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PENDING_REVIEW
                || contentMapper.approve(contentId, LocalDateTime.now()) != 1) {
            throw invalidState("只有待审核资料可以审核通过");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    /** 执行驳回状态流转，仅允许从合法前置状态进入目标状态。 */
    public ContentDetailResponse reject(Long contentId, String reason) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PENDING_REVIEW
                || contentMapper.reject(contentId, reason.trim()) != 1) {
            throw invalidState("只有待审核资料可以驳回");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    /** 执行 takeOffline 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentDetailResponse takeOffline(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.PUBLISHED
                || contentMapper.takeOffline(contentId) != 1) {
            throw invalidState("只有已发布资料可以下架");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    /** 执行 republish 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentDetailResponse republish(Long contentId) {
        LearningContent content = getRequired(contentId);
        if (content.getStatus() != ContentStatus.OFFLINE
                || contentMapper.republish(contentId, LocalDateTime.now()) != 1) {
            throw invalidState("只有已下架资料可以重新发布");
        }
        return detail(getRequired(contentId), true, true);
    }

    @Transactional
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void delete(Long contentId, Long requesterUserId, boolean requesterAdmin) {
        LearningContent content = getRequired(contentId);
        assertOwnerOrAdmin(content, requesterUserId, requesterAdmin);
        assertEditable(content);
        if (contentMapper.softDelete(contentId, content.getPublisherId()) != 1) {
            throw invalidState("只有草稿或已驳回资料可以删除");
        }
    }

    /** 返回Required。 */
    public LearningContent getRequired(Long contentId) {
        return contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
    }

    /** 校验OwnerOr管理及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void assertOwnerOrAdmin(
            LearningContent content,
            Long requesterUserId,
            boolean requesterAdmin
    ) {
        if (!requesterAdmin && !content.getPublisherId().equals(requesterUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权管理其他发布者的资料");
        }
    }

    /** 校验可编辑及相关业务前置条件，不满足时抛出明确业务异常。 */
    public void assertEditable(LearningContent content) {
        if (content.getStatus() != ContentStatus.DRAFT
                && content.getStatus() != ContentStatus.REJECTED) {
            throw invalidState("当前资料状态不允许修改文件或内容");
        }
    }

    /** 执行 applyWriteRequest 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void applyWriteRequest(LearningContent content, ContentWriteRequest request) {
        content.setCategoryId(request.categoryId());
        content.setTitle(request.title().trim());
        content.setSummary(normalize(request.summary()));
        content.setContentType(ContentType.GENERAL);
        //
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

    /** 更新班级Scopes，通过返回值或版本条件识别并发状态变化。 */
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

    /** 转换或规范化价格数据，不引入额外持久化副作用。 */
    private BigDecimal normalizePrice(Boolean free, BigDecimal requestedPrice) {
        if (Boolean.TRUE.equals(free)) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (requestedPrice == null || requestedPrice.compareTo(MINIMUM_PAID_PRICE) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "付费资料价格不能低于0.01元");
        }
        return requestedPrice;
    }

    /** 校验ReadyFor复习及相关业务前置条件，不满足时抛出明确业务异常。 */
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

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 执行 summary 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentSummaryResponse summary(LearningContent content) {
        return ContentSummaryResponse.from(content, coverUrl(content));
    }

    /** 执行 coverUrl 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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

    /** 执行 valueOrZero 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 执行 invalidState 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException invalidState(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
