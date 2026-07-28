/* 文件职责：定义或实现模拟AI 客户端外部调用适配，隔离供应商协议与业务服务。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定义或实现模拟AI 客户端外部调用适配，隔离供应商协议与业务服务。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public class MockAiClient implements AiClient {
    /** 定义 PROVIDER 常量，统一该组件使用的固定规则或默认值。 */
    private static final String PROVIDER = "mock";
    /** 保存model，供该类型的业务逻辑读取或更新。 */
    private final String model;
    /** 保存scenario，供该类型的业务逻辑读取或更新。 */
    private final Scenario scenario;
    /** 保存delay，供该类型的业务逻辑读取或更新。 */
    private final Duration delay;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public MockAiClient(String model) {
        this(model, "success", Duration.ZERO);
    }

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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
    /** 执行 provider 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public String provider() {
        return PROVIDER;
    }

    @Override
    /** 执行 model 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public String model() {
        return model;
    }

    @Override
    /** 执行完成状态流转，仅允许从合法前置状态进入目标状态。 */
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
        } else if (systemInstruction.contains("TASK:OFFLINE_TEACHER_RECOMMENDATION")) {
            Matcher teacherMatcher = Pattern.compile(
                    "\\\"teacherId\\\"\\s*:\\s*(\\d+)"
            ).matcher(lastUserMessage.content());
            String teacherId = teacherMatcher.find()
                    ? teacherMatcher.group(1)
                    : "1";
            content = """
                    {
                      "recommendations": [
                        {
                          "teacherId": %s,
                          "reason": "该教师的教学方向与学习目标较为匹配",
                          "matchHighlights": ["教授内容匹配", "可结合实际情况进一步沟通"]
                        }
                      ]
                    }
                    """.formatted(teacherId);
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

    /** 执行 waitForDelay 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 转换或规范化Scenario数据，不引入额外持久化副作用。 */
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

    /** 执行 jsonEscape 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String jsonEscape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    /** 校验Text及相关业务前置条件，不满足时抛出明确业务异常。 */
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
