/* 文件职责：表示AI会话领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

import java.time.LocalDateTime;

/**
 * 表示AI会话领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class AiConversation extends BaseEntity {
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long contentId;
    /** 保存标题，供该类型的业务逻辑读取或更新。 */
    private String title;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private AiConversationStatus status;
    /** 保存last消息时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime lastMessageAt;

    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long userId) { this.userId = userId; }
    /** 返回学习资料ID。 */
    public Long getContentId() { return contentId; }
    /** 更新学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setContentId(Long contentId) { this.contentId = contentId; }
    /** 返回标题。 */
    public String getTitle() { return title; }
    /** 更新标题；调用方仍需遵守所属领域的校验规则。 */
    public void setTitle(String title) { this.title = title; }
    /** 返回状态。 */
    public AiConversationStatus getStatus() { return status; }
    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(AiConversationStatus status) { this.status = status; }
    /** 返回Last消息时间。 */
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    /** 更新Last消息时间；调用方仍需遵守所属领域的校验规则。 */
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
}
