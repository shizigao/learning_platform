package com.learningplatform.ai.client;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

public class MockAiClient implements AiClient {
    private static final String PROVIDER = "mock";
    private final String model;
    private final Scenario scenario;
    private final Duration delay;

    public MockAiClient(String model) {
        this(model, "success", Duration.ZERO);
    }

    public MockAiClient(String model, String scenario, Duration delay) {
        this.model = requireText(model, "模拟 AI 模型不能为空");
        this.scenario = parseScenario(scenario);
        if (delay == null || delay.isNegative()) {
            throw new AiClientException(
                    AiClientException.Kind.CONFIGURATION,
                    "模拟 AI 延迟不能为负数"
            );
        }
        this.delay = delay;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public AiClientResponse complete(AiClientRequest request) {
        waitForDelay();
        if (scenario == Scenario.FAILURE) {
            throw new AiClientException(
                    AiClientException.Kind.PROVIDER_ERROR,
                    "模拟 AI 失败场景"
            );
        }
        String systemInstruction = request.messages().stream()
                .filter(message -> message.role() == AiRole.SYSTEM)
                .map(AiMessage::content)
                .findFirst()
                .orElse("");
        AiMessage lastUserMessage = request.messages().stream()
                .filter(message -> message.role() == AiRole.USER)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AiClientException(
                        AiClientException.Kind.INVALID_RESPONSE,
                        "模拟 AI 请求缺少用户消息"
                ));
        String preview = lastUserMessage.content().length() <= 80
                ? lastUserMessage.content()
                : lastUserMessage.content().substring(0, 80) + "…";
        String content;
        if (systemInstruction.contains("TASK:CONTENT_SUMMARY")) {
            String escaped = jsonEscape(preview);
            content = """
                    {
                      "summary": "模拟摘要：%s",
                      "knowledgePoints": [
                        "识别资料的核心主题",
                        "梳理关键概念之间的关系",
                        "结合复习提纲巩固文本内容"
                      ],
                      "reviewOutline": "一、核心主题\\n二、关键概念\\n三、复习与自测"
                    }
                    """.formatted(escaped);
        } else if (systemInstruction.contains("TASK:CONTENT_EXPLANATION")) {
            content = "【模拟 AI 讲解】针对“" + preview
                    + "”，请结合资料中的定义、步骤和示例逐项理解。";
        } else {
            content = "【模拟 AI】已处理请求：" + preview;
        }
        return new AiClientResponse(
                PROVIDER,
                model,
                "mock-" + UUID.randomUUID(),
                content,
                "stop",
                0,
                0,
                0
        );
    }

    private void waitForDelay() {
        if (delay.isZero()) {
            return;
        }
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiClientException(
                    AiClientException.Kind.TIMEOUT,
                    "模拟 AI 延迟被中断",
                    exception
            );
        }
    }

    private Scenario parseScenario(String value) {
        String normalized = value == null
                ? "success"
                : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "", "success" -> Scenario.SUCCESS;
            case "failure" -> Scenario.FAILURE;
            case "timeout" -> Scenario.TIMEOUT;
            default -> throw new AiClientException(
                    AiClientException.Kind.CONFIGURATION,
                    "不支持的 AI_MOCK_SCENARIO：" + normalized
            );
        };
    }

    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new AiClientException(AiClientException.Kind.CONFIGURATION, message);
        }
        return value.trim();
    }

    private enum Scenario {
        SUCCESS,
        FAILURE,
        TIMEOUT
    }
}
