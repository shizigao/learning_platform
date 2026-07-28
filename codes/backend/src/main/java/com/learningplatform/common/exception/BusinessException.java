/* 文件职责：表示Business异常失败，携带可由统一异常处理器转换的错误语义。
 * 所属模块：统一协议、异常、配置与跨领域基础设施；所在分层：异常处理层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.common.exception;

import com.learningplatform.common.api.ErrorCode;

/**
 * 表示Business异常失败，携带可由统一异常处理器转换的错误语义。
 *
 * <p>职责边界：遵守 统一协议、异常、配置与跨领域基础设施 模块的职责边界。</p>
 */
public class BusinessException extends RuntimeException {
    /** 保存错误编码，供该类型的业务逻辑读取或更新。 */
    private final ErrorCode errorCode;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /** 返回错误编码。 */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

