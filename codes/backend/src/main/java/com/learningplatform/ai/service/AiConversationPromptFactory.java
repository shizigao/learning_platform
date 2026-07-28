/* 文件职责：集中创建AI会话提示词工厂，保证不同调用场景使用一致规则。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiRole;
import com.learningplatform.ai.domain.AiMessage;
import com.learningplatform.ai.domain.AiMessageRole;
import com.learningplatform.ai.text.ExtractedContentText;
import com.learningplatform.common.config.AiProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
/**
 * 集中创建AI会话提示词工厂，保证不同调用场景使用一致规则。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AiConversationPromptFactory {
    /** 定义 SYSTEM_PROMPT_PREFIX 常量，统一该组件使用的固定规则或默认值。 */
    private static final String SYSTEM_PROMPT_PREFIX = """
            TASK:CONTENT_EXPLANATION
            你是严谨的中文学习讲解助手。只能依据下面提供的资料文本回答。
            如果资料没有给出答案，应明确说明“当前资料未提供相关信息”，不得编造。
            直接回答用户问题，通常控制在 800 个中文字符以内，不展示内部推理过程。

            【资料文本】
            """;

    /** 保存最大ContextMessages，供该类型的业务逻辑读取或更新。 */
    private final int maxContextMessages;
    /** 保存最大ContextChars，供该类型的业务逻辑读取或更新。 */
    private final int maxContextChars;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AiConversationPromptFactory(AiProperties properties) {
        if (properties == null || properties.limits() == null
                || properties.limits().maxContextMessages() <= 0
                || properties.limits().maxContextChars() <= 0) {
            throw new IllegalStateException("AI 上下文限制配置无效");
        }
        this.maxContextMessages = properties.limits().maxContextMessages();
        this.maxContextChars = properties.limits().maxContextChars();
    }

    /** 执行 build 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public List<com.learningplatform.ai.client.AiMessage> build(
            ExtractedContentText source,
            List<AiMessage> history,
            String question
    ) {
        List<AiMessage> selected = selectHistory(history);
        List<com.learningplatform.ai.client.AiMessage> result = new ArrayList<>();
        result.add(new com.learningplatform.ai.client.AiMessage(
                AiRole.SYSTEM,
                SYSTEM_PROMPT_PREFIX + source.text()
        ));
        for (AiMessage message : selected) {
            result.add(new com.learningplatform.ai.client.AiMessage(
                    message.getRole() == AiMessageRole.USER
                            ? AiRole.USER
                            : AiRole.ASSISTANT,
                    message.getContent()
            ));
        }
        result.add(new com.learningplatform.ai.client.AiMessage(
                AiRole.USER,
                question
        ));
        return List.copyOf(result);
    }

    /** 执行 selectHistory 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private List<AiMessage> selectHistory(List<AiMessage> history) {
        List<AiMessage> reversed = new ArrayList<>();
        int remainingChars = maxContextChars;
        for (int index = history.size() - 1;
                index >= 0 && reversed.size() < maxContextMessages
                        && remainingChars > 0;
                index--) {
            AiMessage message = history.get(index);
            if ((message.getRole() != AiMessageRole.USER
                    && message.getRole() != AiMessageRole.ASSISTANT)
                    || message.getContent() == null
                    || message.getContent().isBlank()) {
                continue;
            }
            String content = message.getContent();
            if (content.length() > remainingChars) {
                content = content.substring(content.length() - remainingChars);
            }
            AiMessage limited = new AiMessage();
            limited.setRole(message.getRole());
            limited.setContent(content);
            reversed.add(limited);
            remainingChars -= content.length();
        }
        Collections.reverse(reversed);
        return reversed;
    }
}
