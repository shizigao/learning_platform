/* 文件职责：实现管理AI配置业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiClient;
import com.learningplatform.ai.dto.AdminAiConfigResponse;
import com.learningplatform.common.config.AiProperties;
import org.springframework.stereotype.Service;

@Service
/**
 * 实现管理AI配置业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AdminAiConfigService {
    /** 通过AIClient调用隔离后的外部能力。 */
    private final AiClient aiClient;
    /** 保存配置属性，供该类型的业务逻辑读取或更新。 */
    private final AiProperties properties;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminAiConfigService(AiClient aiClient, AiProperties properties) {
        this.aiClient = aiClient;
        this.properties = properties;
    }

    /** 执行 current 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public AdminAiConfigResponse current() {
        AiProperties.Limits limits = properties.limits();
        AiProperties.DeepSeek deepSeek = properties.deepseek();
        AiProperties.MockProvider mock = properties.mock();
        boolean mockMode = "mock".equalsIgnoreCase(aiClient.provider());
        return new AdminAiConfigResponse(
                aiClient.provider(),
                aiClient.model(),
                mockMode,
                mockMode && mock != null ? safeScenario(mock.scenario()) : null,
                deepSeek != null
                        && deepSeek.apiKey() != null
                        && !deepSeek.apiKey().isBlank(),
                deepSeek == null ? null : deepSeek.baseUrl(),
                deepSeek != null && deepSeek.thinkingEnabled(),
                new AdminAiConfigResponse.Limits(
                        limits.maxInputChars(),
                        limits.maxContextMessages(),
                        limits.maxContextChars(),
                        limits.requestsPerWindow(),
                        limits.rateWindow().toSeconds(),
                        limits.maxConcurrentPerUser(),
                        limits.timeout().toSeconds(),
                        deepSeek == null || deepSeek.connectTimeout() == null
                                ? 0
                                : deepSeek.connectTimeout().toSeconds(),
                        deepSeek == null || deepSeek.timeout() == null
                                ? 0
                                : deepSeek.timeout().toSeconds()
                )
        );
    }

    /** 执行 safeScenario 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String safeScenario(String scenario) {
        return scenario == null || scenario.isBlank()
                ? "success"
                : scenario.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
