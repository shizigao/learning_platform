/* 文件职责：定义操作日志响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：平台治理与管理员操作；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.dto;

import com.learningplatform.admin.audit.OperationLog;
import com.learningplatform.admin.audit.OperationResult;

import java.time.LocalDateTime;

/**
 * 定义操作日志响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record OperationLogResponse(
        Long id,
        Long operatorId,
        String operatorName,
        String module,
        String action,
        String targetType,
        String targetId,
        String requestMethod,
        String requestPath,
        String requestId,
        String ipAddress,
        String userAgent,
        OperationResult result,
        String detailJson,
        String errorMessage,
        Integer durationMs,
        LocalDateTime createdAt
) {
    /** 转换或规范化数据，不引入额外持久化副作用。 */
    public static OperationLogResponse from(OperationLog log) {
        return new OperationLogResponse(
                log.getId(),
                log.getOperatorId(),
                log.getOperatorName(),
                log.getModule(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getRequestMethod(),
                log.getRequestPath(),
                log.getRequestId(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getResult(),
                log.getDetailJson(),
                log.getErrorMessage(),
                log.getDurationMs(),
                log.getCreatedAt()
        );
    }
}
