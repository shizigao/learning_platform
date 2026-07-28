/* 文件职责：实现学习资料Interaction业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.learning.domain.ContentComment;
import com.learningplatform.learning.dto.ContentCommentResponse;
import com.learningplatform.learning.dto.ContentReactionResponse;
import com.learningplatform.learning.dto.CreateCommentRequest;
import com.learningplatform.learning.mapper.ContentCommentMapper;
import com.learningplatform.learning.mapper.ContentInteractionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.learningplatform.user.service.PublicUserProfileCache;

import java.util.List;

@Service
/**
 * 实现学习资料Interaction业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ContentInteractionService {
    /** 访问学习资料持久化数据。 */
    private final LearningContentMapper contentMapper;
    /** 访问interaction持久化数据。 */
    private final ContentInteractionMapper interactionMapper;
    /** 访问评论持久化数据。 */
    private final ContentCommentMapper commentMapper;
    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;
    private final PublicUserProfileCache publicUserProfileCache;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ContentInteractionService(
            LearningContentMapper contentMapper,
            ContentInteractionMapper interactionMapper,
            ContentCommentMapper commentMapper,
            ContentAccessService accessService,
            LearningContentService contentService,
            PublicUserProfileCache publicUserProfileCache
    ) {
        this.contentMapper = contentMapper;
        this.interactionMapper = interactionMapper;
        this.commentMapper = commentMapper;
        this.accessService = accessService;
        this.contentService = contentService;
        this.publicUserProfileCache = publicUserProfileCache;
    }

    /** 执行 state 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentReactionResponse state(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getInteractionVisible(contentId, userId, requesterAdmin);
        return response(content, userId);
    }

    /** 执行 favorites 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public List<ContentSummaryResponse> favorites(Long userId) {
        return interactionMapper.findFavoriteContentIds(userId).stream()
                .map(contentMapper::findById)
                .flatMap(java.util.Optional::stream)
                .filter(content -> content.getStatus() == ContentStatus.PUBLISHED)
                .map(contentService::summary)
                .toList();
    }

    @Transactional
    /** 执行 like 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentReactionResponse like(Long contentId, Long userId, boolean requesterAdmin) {
        LearningContent content = accessService.requireAccess(contentId, userId, requesterAdmin);
        if (interactionMapper.hasLiked(userId, contentId)) {
            throw conflict("不能重复点赞");
        }
        try {
            interactionMapper.insertLike(userId, contentId);
        } catch (DuplicateKeyException exception) {
            throw conflict("不能重复点赞");
        }
        if (contentMapper.incrementLikeCount(contentId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        content.setLikeCount(valueOrZero(content.getLikeCount()) + 1);
        publicUserProfileCache.evictAfterCommit(content.getPublisherId());
        return response(content, userId);
    }

    @Transactional
    /** 执行 unlike 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentReactionResponse unlike(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getInteractionVisible(contentId, userId, requesterAdmin);
        if (interactionMapper.deleteLike(userId, contentId) != 1) {
            throw conflict("尚未点赞该资料");
        }
        contentMapper.decrementLikeCount(contentId);
        content.setLikeCount(Math.max(valueOrZero(content.getLikeCount()) - 1, 0));
        publicUserProfileCache.evictAfterCommit(content.getPublisherId());
        return response(content, userId);
    }

    @Transactional
    /** 执行 favorite 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentReactionResponse favorite(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getInteractionVisible(contentId, userId, requesterAdmin);
        if (interactionMapper.hasFavorited(userId, contentId)) {
            throw conflict("不能重复收藏");
        }
        try {
            interactionMapper.insertFavorite(userId, contentId);
        } catch (DuplicateKeyException exception) {
            throw conflict("不能重复收藏");
        }
        if (contentMapper.incrementFavoriteCount(contentId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        content.setFavoriteCount(valueOrZero(content.getFavoriteCount()) + 1);
        publicUserProfileCache.evictAfterCommit(content.getPublisherId());
        return response(content, userId);
    }

    @Transactional
    /** 执行 unfavorite 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentReactionResponse unfavorite(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getInteractionVisible(contentId, userId, requesterAdmin);
        if (interactionMapper.deleteFavorite(userId, contentId) != 1) {
            throw conflict("尚未收藏该资料");
        }
        contentMapper.decrementFavoriteCount(contentId);
        content.setFavoriteCount(Math.max(valueOrZero(content.getFavoriteCount()) - 1, 0));
        publicUserProfileCache.evictAfterCommit(content.getPublisherId());
        return response(content, userId);
    }

    @Transactional
    /** 执行 comment 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public ContentCommentResponse comment(
            Long contentId,
            Long userId,
            boolean requesterAdmin,
            CreateCommentRequest request
    ) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        if (request.parentId() != null) {
            ContentComment parent = commentMapper.findById(request.parentId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "父评论不存在"));
            if (!contentId.equals(parent.getContentId()) || !"VISIBLE".equals(parent.getStatus())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父评论不属于当前资料或不可见");
            }
        }
        ContentComment comment = new ContentComment();
        comment.setContentId(contentId);
        comment.setUserId(userId);
        comment.setParentId(request.parentId());
        comment.setBody(request.body().trim());
        comment.setStatus("VISIBLE");
        if (commentMapper.insert(comment) != 1 || contentMapper.incrementCommentCount(contentId) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "发布评论失败");
        }
        return ContentCommentResponse.from(
                commentMapper.findById(comment.getId())
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "读取评论失败"))
        );
    }

    /** 执行 comments 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public PageResult<ContentCommentResponse> comments(
            Long contentId,
            Long userId,
            boolean requesterAdmin,
            PageQuery query
    ) {
        getInteractionVisible(contentId, userId, requesterAdmin);
        long total = commentMapper.countVisible(contentId);
        var items = commentMapper.findVisible(contentId, query.offset(), query.getPageSize()).stream()
                .map(ContentCommentResponse::from)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 返回发布。 */
    private LearningContent getPublished(Long contentId) {
        LearningContent content = contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        return content;
    }

    /** 返回InteractionVisible。 */
    private LearningContent getInteractionVisible(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getPublished(contentId);
        if (content.getDistributionMode() == ContentDistributionMode.CLASS) {
            return accessService.requireAccess(contentId, userId, requesterAdmin);
        }
        return content;
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private ContentReactionResponse response(LearningContent content, Long userId) {
        return new ContentReactionResponse(
                interactionMapper.hasLiked(userId, content.getId()),
                interactionMapper.hasFavorited(userId, content.getId()),
                valueOrZero(content.getLikeCount()),
                valueOrZero(content.getFavoriteCount())
        );
    }

    /** 执行 valueOrZero 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    /** 执行 conflict 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
