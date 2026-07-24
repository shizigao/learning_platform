package com.learningplatform.ai.service;

import com.learningplatform.ai.client.AiRole;
import com.learningplatform.ai.domain.AiMessage;
import com.learningplatform.ai.domain.AiMessageRole;
import com.learningplatform.ai.text.ExtractedContentText;
import com.learningplatform.common.config.AiProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiConversationPromptFactoryTests {
    @Test
    void keepsOnlyMostRecentHistoryWithinMessageAndCharacterLimits() {
        AiConversationPromptFactory factory =
                new AiConversationPromptFactory(properties());
        List<AiMessage> history = List.of(
                message(AiMessageRole.USER, "1111"),
                message(AiMessageRole.ASSISTANT, "2222"),
                message(AiMessageRole.USER, "3333"),
                message(AiMessageRole.ASSISTANT, "4444")
        );

        List<com.learningplatform.ai.client.AiMessage> prompt = factory.build(
                new ExtractedContentText(
                        1L,
                        "资料",
                        "资料正文",
                        "version",
                        List.of()
                ),
                history,
                "新问题"
        );

        assertThat(prompt).hasSize(4);
        assertThat(prompt.get(0).role()).isEqualTo(AiRole.SYSTEM);
        assertThat(prompt.get(1).content()).isEqualTo("3");
        assertThat(prompt.get(2).content()).isEqualTo("4444");
        assertThat(prompt.get(3).content()).isEqualTo("新问题");
        assertThat(prompt.subList(1, 3).stream()
                .mapToInt(value -> value.content().length())
                .sum()).isEqualTo(5);
    }

    private AiMessage message(AiMessageRole role, String content) {
        AiMessage message = new AiMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private AiProperties properties() {
        return new AiProperties(
                "mock",
                new AiProperties.MockProvider("test", "success", Duration.ZERO),
                null,
                new AiProperties.Limits(
                        100_000,
                        2,
                        5,
                        10,
                        Duration.ofMinutes(1),
                        1,
                        Duration.ofSeconds(1)
                )
        );
    }
}
