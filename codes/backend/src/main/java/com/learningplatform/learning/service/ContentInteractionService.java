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

import java.util.List;

@Service
public class ContentInteractionService {
    private final LearningContentMapper contentMapper;
    private final ContentInteractionMapper interactionMapper;
    private final ContentCommentMapper commentMapper;
    private final ContentAccessService accessService;
    private final LearningContentService contentService;

    public ContentInteractionService(
            LearningContentMapper contentMapper,
            ContentInteractionMapper interactionMapper,
            ContentCommentMapper commentMapper,
            ContentAccessService accessService,
            LearningContentService contentService
    ) {
        this.contentMapper = contentMapper;
        this.interactionMapper = interactionMapper;
        this.commentMapper = commentMapper;
        this.accessService = accessService;
        this.contentService = contentService;
    }

    public ContentReactionResponse state(
            Long contentId,
            Long userId,
            boolean requesterAdmin
    ) {
        LearningContent content = getInteractionVisible(contentId, userId, requesterAdmin);
        return response(content, userId);
    }

    public List<ContentSummaryResponse> favorites(Long userId) {
        return interactionMapper.findFavoriteContentIds(userId).stream()
                .map(contentMapper::findById)
                .flatMap(java.util.Optional::stream)
                .filter(content -> content.getStatus() == ContentStatus.PUBLISHED)
                .map(contentService::summary)
                .toList();
    }

    @Transactional
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
        return response(content, userId);
    }

    @Transactional
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
        return response(content, userId);
    }

    @Transactional
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
        return response(content, userId);
    }

    @Transactional
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
        return response(content, userId);
    }

    @Transactional
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

    private LearningContent getPublished(Long contentId) {
        LearningContent content = contentMapper.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在"));
        if (content.getStatus() != ContentStatus.PUBLISHED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "学习资料不存在或尚未发布");
        }
        return content;
    }

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

    private ContentReactionResponse response(LearningContent content, Long userId) {
        return new ContentReactionResponse(
                interactionMapper.hasLiked(userId, content.getId()),
                interactionMapper.hasFavorited(userId, content.getId()),
                valueOrZero(content.getLikeCount()),
                valueOrZero(content.getFavoriteCount())
        );
    }

    private long valueOrZero(Long value) {
        return value == null ? 0 : value;
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
