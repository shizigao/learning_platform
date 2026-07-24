package com.learningplatform.common.api;

import org.slf4j.MDC;

import java.time.Instant;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        Instant timestamp,
        String traceId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data,
                Instant.now(), MDC.get("traceId"));
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage(), null);
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode, message, null);
    }

    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(errorCode.getCode(), message, data, Instant.now(), MDC.get("traceId"));
    }
}

