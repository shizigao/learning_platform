/* 文件职责：表示学习资料领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示学习资料领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class LearningContent extends BaseEntity {
    /** 保存发布者ID，供该类型的业务逻辑读取或更新。 */
    private Long publisherId;
    /** 保存发布者名称，供该类型的业务逻辑读取或更新。 */
    private String publisherName;
    /** 保存分类ID，供该类型的业务逻辑读取或更新。 */
    private Long categoryId;
    /** 保存分类名称，供该类型的业务逻辑读取或更新。 */
    private String categoryName;
    /** 保存标题，供该类型的业务逻辑读取或更新。 */
    private String title;
    /** 保存总结，供该类型的业务逻辑读取或更新。 */
    private String summary;
    /** 保存学习资料类型，供该类型的业务逻辑读取或更新。 */
    private ContentType contentType;
    /** 保存文章正文，供该类型的业务逻辑读取或更新。 */
    private String articleBody;
    /** 保存封面文件ID，供该类型的业务逻辑读取或更新。 */
    private Long coverFileId;
    /** 保存发放模式，供该类型的业务逻辑读取或更新。 */
    private ContentDistributionMode distributionMode;
    /** 保存免费状态，供该类型的业务逻辑读取或更新。 */
    private Boolean free;
    /** 保存价格，供该类型的业务逻辑读取或更新。 */
    private BigDecimal price;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ContentStatus status;
    /** 保存驳回原因，供该类型的业务逻辑读取或更新。 */
    private String rejectionReason;
    /** 保存浏览数量，供该类型的业务逻辑读取或更新。 */
    private Long viewCount;
    /** 保存点赞数量，供该类型的业务逻辑读取或更新。 */
    private Long likeCount;
    /** 保存收藏数量，供该类型的业务逻辑读取或更新。 */
    private Long favoriteCount;
    /** 保存评论数量，供该类型的业务逻辑读取或更新。 */
    private Long commentCount;
    /** 保存提交时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime submittedAt;
    /** 保存发布时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime publishedAt;

    /** 返回发布者ID。 */
    public Long getPublisherId() {
        return publisherId;
    }

    /** 更新发布者ID；调用方仍需遵守所属领域的校验规则。 */
    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    /** 返回发布者名称。 */
    public String getPublisherName() {
        return publisherName;
    }

    /** 更新发布者名称；调用方仍需遵守所属领域的校验规则。 */
    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    /** 返回分类ID。 */
    public Long getCategoryId() {
        return categoryId;
    }

    /** 更新分类ID；调用方仍需遵守所属领域的校验规则。 */
    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    /** 返回分类名称。 */
    public String getCategoryName() {
        return categoryName;
    }

    /** 更新分类名称；调用方仍需遵守所属领域的校验规则。 */
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    /** 返回标题。 */
    public String getTitle() {
        return title;
    }

    /** 更新标题；调用方仍需遵守所属领域的校验规则。 */
    public void setTitle(String title) {
        this.title = title;
    }

    /** 返回总结。 */
    public String getSummary() {
        return summary;
    }

    /** 更新总结；调用方仍需遵守所属领域的校验规则。 */
    public void setSummary(String summary) {
        this.summary = summary;
    }

    /** 返回学习资料类型。 */
    public ContentType getContentType() {
        return contentType;
    }

    /** 更新学习资料类型；调用方仍需遵守所属领域的校验规则。 */
    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    /** 返回文章正文。 */
    public String getArticleBody() {
        return articleBody;
    }

    /** 更新文章正文；调用方仍需遵守所属领域的校验规则。 */
    public void setArticleBody(String articleBody) {
        this.articleBody = articleBody;
    }

    /** 返回封面文件ID。 */
    public Long getCoverFileId() {
        return coverFileId;
    }

    /** 更新封面文件ID；调用方仍需遵守所属领域的校验规则。 */
    public void setCoverFileId(Long coverFileId) {
        this.coverFileId = coverFileId;
    }

    /** 返回发放模式。 */
    public ContentDistributionMode getDistributionMode() {
        return distributionMode;
    }

    /** 更新发放模式；调用方仍需遵守所属领域的校验规则。 */
    public void setDistributionMode(ContentDistributionMode distributionMode) {
        this.distributionMode = distributionMode;
    }

    /** 返回免费状态。 */
    public Boolean getFree() {
        return free;
    }

    /** 更新免费状态；调用方仍需遵守所属领域的校验规则。 */
    public void setFree(Boolean free) {
        this.free = free;
    }

    /** 返回价格。 */
    public BigDecimal getPrice() {
        return price;
    }

    /** 更新价格；调用方仍需遵守所属领域的校验规则。 */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /** 返回状态。 */
    public ContentStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    /** 返回驳回原因。 */
    public String getRejectionReason() {
        return rejectionReason;
    }

    /** 更新驳回原因；调用方仍需遵守所属领域的校验规则。 */
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    /** 返回浏览数量。 */
    public Long getViewCount() {
        return viewCount;
    }

    /** 更新浏览数量；调用方仍需遵守所属领域的校验规则。 */
    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    /** 返回点赞数量。 */
    public Long getLikeCount() {
        return likeCount;
    }

    /** 更新点赞数量；调用方仍需遵守所属领域的校验规则。 */
    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    /** 返回收藏数量。 */
    public Long getFavoriteCount() {
        return favoriteCount;
    }

    /** 更新收藏数量；调用方仍需遵守所属领域的校验规则。 */
    public void setFavoriteCount(Long favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    /** 返回评论数量。 */
    public Long getCommentCount() {
        return commentCount;
    }

    /** 更新评论数量；调用方仍需遵守所属领域的校验规则。 */
    public void setCommentCount(Long commentCount) {
        this.commentCount = commentCount;
    }

    /** 返回提交时间。 */
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    /** 更新提交时间；调用方仍需遵守所属领域的校验规则。 */
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    /** 返回发布时间。 */
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    /** 更新发布时间；调用方仍需遵守所属领域的校验规则。 */
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
