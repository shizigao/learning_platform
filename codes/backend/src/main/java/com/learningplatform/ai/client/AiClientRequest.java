/* 文件职责：定义AI 客户端请求接口的请求字段和 Bean Validation 约束。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

import java.util.List;

/**
 * 定义AI 客户端请求接口的请求字段和 Bean Validation 约束。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public record AiClientRequest(
        List<AiMessage> messages,
        Integer maxOutputTokens,
        Double temperature,
        AiResponseFormat responseFormat
) {
    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiClientRequest(
            List<AiMessage> messages,
            Integer maxOutputTokens,
            Double temperature
    ) {
        this(messages, maxOutputTokens, temperature, AiResponseFormat.TEXT);
    }

    public AiClientRequest {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("AI 请求至少需要一条消息");
        }
        messages = List.copyOf(messages);
        if (maxOutputTokens != null && maxOutputTokens <= 0) {
            throw new IllegalArgumentException("AI 最大输出长度必须大于0");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("AI temperature 必须在0到2之间");
        }
        if (responseFormat == null) {
            responseFormat = AiResponseFormat.TEXT;
        }
    }
}
