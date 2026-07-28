/* 文件职责：定义Api响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：模块根目录。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.api;

import org.slf4j.MDC;

import java.time.Instant;

/**
 * 所有 JSON 接口的统一响应壳。
 *
 * @param code 业务码，成功固定为 {@code 0}，不等同于 HTTP 状态码
 * @param message 可直接展示或记录的简短结果信息
 * @param data 业务载荷；无返回数据时为 {@code null}
 * @param timestamp 服务端生成响应的 UTC 时间
 * @param traceId 当前请求的链路追踪号，用于关联后端日志
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        Instant timestamp,
        String traceId
) {
    /** 构造成功响应，并从 MDC 自动带出当前 traceId。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data,
                Instant.now(), MDC.get("traceId"));
    }

    /** 构造无业务载荷的成功响应。 */
    public static ApiResponse<Void> success() {
        return success(null);
    }

    /** 使用错误码的默认文案构造失败响应。 */
    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage(), null);
    }

    /** 使用面向当前场景的安全文案构造失败响应。 */
    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return failure(errorCode, message, null);
    }

    /** 构造可附带字段级错误等结构化数据的失败响应。 */
    public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message, T data) {
        return new ApiResponse<>(errorCode.getCode(), message, data, Instant.now(), MDC.get("traceId"));
    }
}
