package com.learningplatform.learning.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.learning.domain.LearningProgress;
import com.learningplatform.learning.dto.LearningProgressResponse;
import com.learningplatform.learning.dto.UpdateLearningProgressRequest;
import com.learningplatform.learning.mapper.LearningProgressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LearningProgressService {
    private final LearningProgressMapper progressMapper;
    private final ContentAccessService accessService;

    public LearningProgressService(
            LearningProgressMapper progressMapper,
            ContentAccessService accessService
    ) {
        this.progressMapper = progressMapper;
        this.accessService = accessService;
    }

    @Transactional
    public LearningProgressResponse start(Long userId, boolean requesterAdmin, Long contentId) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        return progressMapper.find(userId, contentId)
                .map(existing -> touch(existing, userId, contentId))
                .orElseGet(() -> create(userId, contentId));
    }

    @Transactional
    public LearningProgressResponse update(
            Long userId,
            boolean requesterAdmin,
            Long contentId,
            UpdateLearningProgressRequest request
    ) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        LearningProgress existing = progressMapper.find(userId, contentId)
                .orElseGet(() -> createProgress(userId, contentId));
        BigDecimal progressPercent = request.progressPercent().setScale(2, RoundingMode.HALF_UP);
        String lastPosition = normalize(request.lastPosition());
        if (progressMapper.update(
                userId,
                contentId,
                progressPercent,
                lastPosition,
                LocalDateTime.now()
        ) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新学习进度失败");
        }
        return getRequired(userId, contentId);
    }

    public LearningProgressResponse get(Long userId, Long contentId) {
        return getRequired(userId, contentId);
    }

    public List<LearningProgressResponse> list(Long userId) {
        return progressMapper.findByUserId(userId).stream()
                .map(LearningProgressResponse::from)
                .toList();
    }

    private LearningProgressResponse touch(
            LearningProgress existing,
            Long userId,
            Long contentId
    ) {
        if (progressMapper.update(
                userId,
                contentId,
                existing.getProgressPercent(),
                existing.getLastPosition(),
                LocalDateTime.now()
        ) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "更新最后学习时间失败");
        }
        return getRequired(userId, contentId);
    }

    private LearningProgressResponse create(Long userId, Long contentId) {
        createProgress(userId, contentId);
        return getRequired(userId, contentId);
    }

    private LearningProgress createProgress(Long userId, Long contentId) {
        LocalDateTime now = LocalDateTime.now();
        LearningProgress progress = new LearningProgress();
        progress.setUserId(userId);
        progress.setContentId(contentId);
        progress.setStartedAt(now);
        progress.setLastLearnedAt(now);
        progress.setProgressPercent(BigDecimal.ZERO.setScale(2));
        if (progressMapper.insert(progress) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "开始学习失败");
        }
        return progress;
    }

    private LearningProgressResponse getRequired(Long userId, Long contentId) {
        return progressMapper.find(userId, contentId)
                .map(LearningProgressResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "尚未开始学习该资料"));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
