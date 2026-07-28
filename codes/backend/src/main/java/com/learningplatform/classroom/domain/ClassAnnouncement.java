/* 文件职责：表示班级公告领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示班级公告领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ClassAnnouncement extends BaseEntity {
    /** 保存班级ID，供该类型的业务逻辑读取或更新。 */
    private Long classId;
    /** 保存authorID，供该类型的业务逻辑读取或更新。 */
    private Long authorId;
    /** 保存author名称，供该类型的业务逻辑读取或更新。 */
    private String authorName;
    /** 保存标题，供该类型的业务逻辑读取或更新。 */
    private String title;
    /** 保存正文，供该类型的业务逻辑读取或更新。 */
    private String body;
    /** 保存pinned，供该类型的业务逻辑读取或更新。 */
    private Boolean pinned;

    /** 返回班级ID。 */
    public Long getClassId() { return classId; }
    /** 更新班级ID；调用方仍需遵守所属领域的校验规则。 */
    public void setClassId(Long classId) { this.classId = classId; }
    /** 返回AuthorID。 */
    public Long getAuthorId() { return authorId; }
    /** 更新AuthorID；调用方仍需遵守所属领域的校验规则。 */
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    /** 返回Author名称。 */
    public String getAuthorName() { return authorName; }
    /** 更新Author名称；调用方仍需遵守所属领域的校验规则。 */
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    /** 返回标题。 */
    public String getTitle() { return title; }
    /** 更新标题；调用方仍需遵守所属领域的校验规则。 */
    public void setTitle(String title) { this.title = title; }
    /** 返回正文。 */
    public String getBody() { return body; }
    /** 更新正文；调用方仍需遵守所属领域的校验规则。 */
    public void setBody(String body) { this.body = body; }
    /** 返回Pinned。 */
    public Boolean getPinned() { return pinned; }
    /** 更新Pinned；调用方仍需遵守所属领域的校验规则。 */
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
}
