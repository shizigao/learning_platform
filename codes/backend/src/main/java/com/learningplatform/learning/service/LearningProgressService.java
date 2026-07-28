/* 文件职责：实现学习进度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现学习进度业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class LearningProgressService {
    /** 访问进度持久化数据。 */
    private final LearningProgressMapper progressMapper;
    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public LearningProgressService(
            LearningProgressMapper progressMapper,
            ContentAccessService accessService
    ) {
        this.progressMapper = progressMapper;
        this.accessService = accessService;
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public LearningProgressResponse start(Long userId, boolean requesterAdmin, Long contentId) {
        accessService.requireAccess(contentId, userId, requesterAdmin);
        return progressMapper.find(userId, contentId)
                .map(existing -> touch(existing, userId, contentId))
                .orElseGet(() -> create(userId, contentId));
    }

    @Transactional
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
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

    /** 执行 get 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public LearningProgressResponse get(Long userId, Long contentId) {
        return getRequired(userId, contentId);
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public List<LearningProgressResponse> list(Long userId) {
        return progressMapper.findByUserId(userId).stream()
                .map(LearningProgressResponse::from)
                .toList();
    }

    /** 转换或规范化uch数据，不引入额外持久化副作用。 */
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

    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    private LearningProgressResponse create(Long userId, Long contentId) {
        createProgress(userId, contentId);
        return getRequired(userId, contentId);
    }

    /** 创建或初始化进度，并维护唯一性、初始状态和必要关联。 */
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

    /** 返回Required。 */
    private LearningProgressResponse getRequired(Long userId, Long contentId) {
        return progressMapper.find(userId, contentId)
                .map(LearningProgressResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "尚未开始学习该资料"));
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
