/* 文件职责：表示学习进度领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示学习进度领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class LearningProgress extends BaseEntity {
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long contentId;
    /** 保存started时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime startedAt;
    /** 保存lastLearned时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime lastLearnedAt;
    /** 保存进度Percent，供该类型的业务逻辑读取或更新。 */
    private BigDecimal progressPercent;
    /** 保存lastPosition，供该类型的业务逻辑读取或更新。 */
    private String lastPosition;
    /** 保存completed时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime completedAt;

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回学习资料ID。 */
    public Long getContentId() {
        return contentId;
    }

    /** 更新学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    /** 返回Started时间。 */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** 更新Started时间；调用方仍需遵守所属领域的校验规则。 */
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    /** 返回LastLearned时间。 */
    public LocalDateTime getLastLearnedAt() {
        return lastLearnedAt;
    }

    /** 更新LastLearned时间；调用方仍需遵守所属领域的校验规则。 */
    public void setLastLearnedAt(LocalDateTime lastLearnedAt) {
        this.lastLearnedAt = lastLearnedAt;
    }

    /** 返回进度Percent。 */
    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    /** 更新进度Percent；调用方仍需遵守所属领域的校验规则。 */
    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    /** 返回LastPosition。 */
    public String getLastPosition() {
        return lastPosition;
    }

    /** 更新LastPosition；调用方仍需遵守所属领域的校验规则。 */
    public void setLastPosition(String lastPosition) {
        this.lastPosition = lastPosition;
    }

    /** 返回Completed时间。 */
    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    /** 更新Completed时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
