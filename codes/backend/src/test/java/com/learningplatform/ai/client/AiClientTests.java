package com.learningplatform.ai.client;

import com.learningplatform.ai.config.AiClientConfiguration;
import com.learningplatform.common.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiClientTests {
    @Test
    void selectsDeterministicMockWithoutRealApiKey() {
        AiClient client = new AiClientConfiguration().aiClient(properties(
                "mock",
                "",
                "mock-learning-assistant-test"
        ));

        AiClientResponse response = client.complete(new AiClientRequest(
                List.of(
                        new AiMessage(AiRole.SYSTEM, "你是学习助手"),
                        new AiMessage(AiRole.USER, "请总结数据库事务")
                ),
                500,
                0.2
        ));

        assertThat(client).isInstanceOf(MockAiClient.class);
        assertThat(response.provider()).isEqualTo("mock");
        assertThat(response.model()).isEqualTo("mock-learning-assistant-test");
        assertThat(response.content()).contains("模拟 AI", "请总结数据库事务");
        assertThat(response.externalRequestId()).startsWith("mock-");
    }

    @Test
    void selectsDeepSeekOnlyWhenExplicitlyConfigured() {
        AiClient client = new AiClientConfiguration().aiClient(properties(
                "deepseek",
                "test-only-key",
                "mock-learning-assistant-test"
        ));

        assertThat(client).isInstanceOf(DeepSeekAiClient.class);
        assertThat(client.provider()).isEqualTo("deepseek");
        assertThat(client.model()).isEqualTo("deepseek-v4-flash");
    }

    @Test
    void failsFastWithoutDeepSeekApiKeyAndRejectsUnknownProvider() {
        assertThatThrownBy(() -> new AiClientConfiguration().aiClient(properties(
                "deepseek",
                "",
                "mock-learning-assistant-test"
        )))
                .isInstanceOfSatisfying(AiClientException.class, exception -> {
                    assertThat(exception.getKind())
                            .isEqualTo(AiClientException.Kind.CONFIGURATION);
                    assertThat(exception.getMessage()).contains("DEEPSEEK_API_KEY");
                });

        assertThatThrownBy(() -> new AiClientConfiguration().aiClient(properties(
                "unknown",
                "",
                "mock-learning-assistant-test"
        )))
                .isInstanceOfSatisfying(AiClientException.class, exception ->
                        assertThat(exception.getKind())
                                .isEqualTo(AiClientException.Kind.CONFIGURATION)
                );
    }

    @Test
    void mapsDeepSeekChatCompletionWithoutExposingKey() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-only-key"))
                .andExpect(content().json("""
                        {
                          "model": "deepseek-v4-flash",
                          "messages": [
                            {"role": "system", "content": "你是学习助手"},
                            {"role": "user", "content": "解释 ACID"}
                          ],
                          "stream": false,
                          "max_tokens": 800,
                          "temperature": 0.3,
                          "response_format": {"type": "json_object"},
                          "thinking": {"type": "disabled"}
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "id": "provider-request-1",
                          "model": "deepseek-v4-flash",
                          "choices": [{
                            "message": {
                              "role": "assistant",
                              "content": "ACID 包括原子性、一致性、隔离性和持久性。"
                            },
                            "finish_reason": "stop"
                          }],
                          "usage": {
                            "prompt_tokens": 20,
                            "completion_tokens": 15,
                            "total_tokens": 35
                          }
                        }
                        """, MediaType.APPLICATION_JSON));
        DeepSeekAiClient client = new DeepSeekAiClient(
                properties("deepseek", "test-only-key", "mock")
                        .deepseek(),
                builder
        );

        AiClientResponse response = client.complete(new AiClientRequest(
                List.of(
                        new AiMessage(AiRole.SYSTEM, "你是学习助手"),
                        new AiMessage(AiRole.USER, "解释 ACID")
                ),
                800,
                0.3,
                AiResponseFormat.JSON_OBJECT
        ));

        assertThat(response.provider()).isEqualTo("deepseek");
        assertThat(response.externalRequestId()).isEqualTo("provider-request-1");
        assertThat(response.content()).contains("原子性", "持久性");
        assertThat(response.totalTokens()).isEqualTo(35);
        server.verify();
    }

    private AiProperties properties(
            String provider,
            String apiKey,
            String mockModel
    ) {
        return new AiProperties(
                provider,
                new AiProperties.MockProvider(
                        mockModel,
                        "success",
                        Duration.ZERO
                ),
                new AiProperties.DeepSeek(
                        "https://api.deepseek.com/",
                        apiKey,
                        "deepseek-v4-flash",
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(5),
                        false
                ),
                null
        );
    }
}
