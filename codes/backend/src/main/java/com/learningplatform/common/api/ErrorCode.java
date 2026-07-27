package com.learningplatform.common.api;

import org.springframework.http.HttpStatus;

/**
 * 平台级稳定错误码。
 *
 * <p>HTTP 状态表达协议语义，五位业务码便于前端精确分支和日志检索。
 * 领域服务可覆盖提示文案，但不得改变同一枚举值对应的 HTTP 状态。</p>
 */
public enum ErrorCode {
    SUCCESS(0, "操作成功", HttpStatus.OK),
    BAD_REQUEST(40000, "请求参数错误", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(40001, "参数校验失败", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "请先登录", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "无权执行此操作", HttpStatus.FORBIDDEN),
    NOT_FOUND(40400, "请求的资源不存在", HttpStatus.NOT_FOUND),
    CONFLICT(40900, "当前状态下无法执行此操作", HttpStatus.CONFLICT),
    PAYLOAD_TOO_LARGE(41300, "上传文件大小超出限制", HttpStatus.PAYLOAD_TOO_LARGE),
    TOO_MANY_REQUESTS(42900, "请求过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    INTERNAL_ERROR(50000, "系统繁忙，请稍后重试", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    /** 绑定业务码、默认安全文案和 HTTP 状态。 */
    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
