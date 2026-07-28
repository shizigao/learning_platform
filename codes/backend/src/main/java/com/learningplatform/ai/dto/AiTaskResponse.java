/* 文件职责：定义AI任务响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

import com.learningplatform.ai.domain.AiTask;
import com.learningplatform.ai.domain.AiTaskStatus;
import com.learningplatform.ai.domain.AiTaskType;

import java.time.LocalDateTime;

/**
 * 定义AI任务响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AiTaskResponse(
        Long id,
        String requestId,
        Long contentId,
        Long conversationId,
        AiTaskType taskType,
        String provider,
        String model,
        AiTaskStatus status,
        Integer inputChars,
        Integer quotaCost,
        String errorCode,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static AiTaskResponse from(AiTask task) {
        return new AiTaskResponse(
                task.getId(),
                task.getRequestId(),
                task.getContentId(),
                task.getConversationId(),
                task.getTaskType(),
                task.getProvider(),
                task.getModel(),
                task.getStatus(),
                task.getInputChars(),
                task.getQuotaCost(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getStartedAt(),
                task.getFinishedAt(),
                task.getCreatedAt()
        );
    }
}
