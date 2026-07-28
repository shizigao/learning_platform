/* 文件职责：表示AI 客户端异常失败，携带可由统一异常处理器转换的错误语义。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

/**
 * 表示AI 客户端异常失败，携带可由统一异常处理器转换的错误语义。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public class AiClientException extends RuntimeException {
    /** 保存kind，供该类型的业务逻辑读取或更新。 */
    private final Kind kind;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /** 返回Kind。 */
    public Kind getKind() {
        return kind;
    }

    public enum Kind {
        CONFIGURATION,
        AUTHENTICATION,
        RATE_LIMIT,
        TIMEOUT,
        PROVIDER_ERROR,
        INVALID_RESPONSE
    }
}
