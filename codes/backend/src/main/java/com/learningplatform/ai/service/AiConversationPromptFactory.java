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
public class AiConversationPromptFactory {
    private static final String SYSTEM_PROMPT_PREFIX = """
            TASK:CONTENT_EXPLANATION
            你是严谨的中文学习讲解助手。只能依据下面提供的资料文本回答。
            如果资料没有给出答案，应明确说明“当前资料未提供相关信息”，不得编造。
            直接回答用户问题，通常控制在 800 个中文字符以内，不展示内部推理过程。

            【资料文本】
            """;

    private final int maxContextMessages;
    private final int maxContextChars;

    public AiConversationPromptFactory(AiProperties properties) {
        if (properties == null || properties.limits() == null
                || properties.limits().maxContextMessages() <= 0
                || properties.limits().maxContextChars() <= 0) {
            throw new IllegalStateException("AI 上下文限制配置无效");
        }
        this.maxContextMessages = properties.limits().maxContextMessages();
        this.maxContextChars = properties.limits().maxContextChars();
    }

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
