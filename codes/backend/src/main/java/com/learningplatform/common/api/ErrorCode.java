/* 文件职责：枚举错误编码允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：模块根目录。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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

    /** 保存编码，供该类型的业务逻辑读取或更新。 */
    private final int code;
    /** 保存消息，供该类型的业务逻辑读取或更新。 */
    private final String message;
    /** 保存http状态，供该类型的业务逻辑读取或更新。 */
    private final HttpStatus httpStatus;

    /** 绑定业务码、默认安全文案和 HTTP 状态。 */
    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    /** 返回编码。 */
    public int getCode() {
        return code;
    }

    /** 返回消息。 */
    public String getMessage() {
        return message;
    }

    /** 返回Http状态。 */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
