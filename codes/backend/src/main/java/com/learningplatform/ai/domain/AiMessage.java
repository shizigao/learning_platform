/* 文件职责：表示AI消息领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示AI消息领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class AiMessage extends BaseEntity {
    /** 保存会话ID，供该类型的业务逻辑读取或更新。 */
    private Long conversationId;
    /** 保存任务ID，供该类型的业务逻辑读取或更新。 */
    private Long taskId;
    /** 保存角色，供该类型的业务逻辑读取或更新。 */
    private AiMessageRole role;
    /** 保存学习资料，供该类型的业务逻辑读取或更新。 */
    private String content;
    /** 保存sequenceNo，供该类型的业务逻辑读取或更新。 */
    private Integer sequenceNo;
    /** 保存令牌数量，供该类型的业务逻辑读取或更新。 */
    private Integer tokenCount;

    /** 返回会话ID。 */
    public Long getConversationId() { return conversationId; }
    /** 更新会话ID；调用方仍需遵守所属领域的校验规则。 */
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    /** 返回任务ID。 */
    public Long getTaskId() { return taskId; }
    /** 更新任务ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    /** 返回角色。 */
    public AiMessageRole getRole() { return role; }
    /** 更新角色；调用方仍需遵守所属领域的校验规则。 */
    public void setRole(AiMessageRole role) { this.role = role; }
    /** 返回学习资料。 */
    public String getContent() { return content; }
    /** 更新学习资料；调用方仍需遵守所属领域的校验规则。 */
    public void setContent(String content) { this.content = content; }
    /** 返回SequenceNo。 */
    public Integer getSequenceNo() { return sequenceNo; }
    /** 更新SequenceNo；调用方仍需遵守所属领域的校验规则。 */
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    /** 返回令牌数量。 */
    public Integer getTokenCount() { return tokenCount; }
    /** 更新令牌数量；调用方仍需遵守所属领域的校验规则。 */
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
}
