/* 文件职责：定义管理AI配置响应接口的只读返回契约，避免直接暴露数据库实体。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.dto;

/**
 * 定义管理AI配置响应接口的只读返回契约，避免直接暴露数据库实体。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public record AdminAiConfigResponse(
        String provider,
        String model,
        boolean mockMode,
        String mockScenario,
        boolean apiKeyConfigured,
        String baseUrl,
        boolean thinkingEnabled,
        Limits limits
) {
    /** 执行 Limits 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public record Limits(
            int maxInputChars,
            int maxContextMessages,
            int maxContextChars,
            int requestsPerWindow,
            long rateWindowSeconds,
            int maxConcurrentPerUser,
            long requestTimeoutSeconds,
            long providerConnectTimeoutSeconds,
            long providerTimeoutSeconds
    ) {
    }
}
