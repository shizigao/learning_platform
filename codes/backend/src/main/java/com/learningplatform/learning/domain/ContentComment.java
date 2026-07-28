/* 文件职责：表示学习资料评论领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：学习进度、点赞、收藏与评论；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.learning.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示学习资料评论领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ContentComment extends BaseEntity {
    /** 保存学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long contentId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存parentID，供该类型的业务逻辑读取或更新。 */
    private Long parentId;
    /** 保存正文，供该类型的业务逻辑读取或更新。 */
    private String body;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private String status;

    /** 返回学习资料ID。 */
    public Long getContentId() {
        return contentId;
    }

    /** 更新学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setContentId(Long contentId) {
        this.contentId = contentId;
    }

    /** 返回用户ID。 */
    public Long getUserId() {
        return userId;
    }

    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /** 返回ParentID。 */
    public Long getParentId() {
        return parentId;
    }

    /** 更新ParentID；调用方仍需遵守所属领域的校验规则。 */
    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /** 返回正文。 */
    public String getBody() {
        return body;
    }

    /** 更新正文；调用方仍需遵守所属领域的校验规则。 */
    public void setBody(String body) {
        this.body = body;
    }

    /** 返回状态。 */
    public String getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(String status) {
        this.status = status;
    }
}
