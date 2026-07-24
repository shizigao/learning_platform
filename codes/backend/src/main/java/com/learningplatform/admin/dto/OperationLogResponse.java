package com.learningplatform.admin.dto;

import com.learningplatform.admin.audit.OperationLog;
import com.learningplatform.admin.audit.OperationResult;

import java.time.LocalDateTime;

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
