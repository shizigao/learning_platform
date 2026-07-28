/* 文件职责：表示学习资料PublicationStats领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.domain;

/**
 * 表示学习资料PublicationStats领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ContentPublicationStats {
    /** 保存学习资料数量，供该类型的业务逻辑读取或更新。 */
    private Long contentCount;
    /** 保存浏览数量，供该类型的业务逻辑读取或更新。 */
    private Long viewCount;
    /** 保存点赞数量，供该类型的业务逻辑读取或更新。 */
    private Long likeCount;
    /** 保存收藏数量，供该类型的业务逻辑读取或更新。 */
    private Long favoriteCount;

    /** 返回学习资料数量。 */
    public Long getContentCount() { return contentCount; }
    /** 更新学习资料数量；调用方仍需遵守所属领域的校验规则。 */
    public void setContentCount(Long contentCount) { this.contentCount = contentCount; }
    /** 返回浏览数量。 */
    public Long getViewCount() { return viewCount; }
    /** 更新浏览数量；调用方仍需遵守所属领域的校验规则。 */
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    /** 返回点赞数量。 */
    public Long getLikeCount() { return likeCount; }
    /** 更新点赞数量；调用方仍需遵守所属领域的校验规则。 */
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }
    /** 返回收藏数量。 */
    public Long getFavoriteCount() { return favoriteCount; }
    /** 更新收藏数量；调用方仍需遵守所属领域的校验规则。 */
    public void setFavoriteCount(Long favoriteCount) { this.favoriteCount = favoriteCount; }
}
