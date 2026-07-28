/* 文件职责：定义或实现DeepSeek AI 客户端外部调用适配，隔离供应商协议与业务服务。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.common.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * 定义或实现DeepSeek AI 客户端外部调用适配，隔离供应商协议与业务服务。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public class DeepSeekAiClient implements AiClient {
    /** 定义 PROVIDER 常量，统一该组件使用的固定规则或默认值。 */
    private static final String PROVIDER = "deepseek";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiClient.class);
    /** 保存model，供该类型的业务逻辑读取或更新。 */
    private final String model;
    /** 保存endpoint，供该类型的业务逻辑读取或更新。 */
    private final String endpoint;
    /** 保存thinking启用状态，供该类型的业务逻辑读取或更新。 */
    private final boolean thinkingEnabled;
    /** 通过restClient调用隔离后的外部能力。 */
    private final RestClient restClient;
    /** 访问object持久化数据。 */
    private final ObjectMapper objectMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public DeepSeekAiClient(AiProperties.DeepSeek properties) {
        this(properties, RestClient.builder(), true);
    }

    DeepSeekAiClient(
            AiProperties.DeepSeek properties,
            RestClient.Builder builder
    ) {
        this(properties, builder, false);
    }

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    private DeepSeekAiClient(
            AiProperties.DeepSeek properties,
            RestClient.Builder builder,
            boolean configureTimeout
    ) {
        if (properties == null) {
            throw configuration("DeepSeek 配置不能为空");
        }
        String baseUrl = requireText(properties.baseUrl(), "DeepSeek Base URL 不能为空");
        String apiKey = requireText(properties.apiKey(), "启用 DeepSeek 时必须配置 DEEPSEEK_API_KEY");
        this.model = requireText(properties.model(), "DeepSeek 模型不能为空");
        this.endpoint = stripTrailingSlash(baseUrl) + "/chat/completions";
        this.thinkingEnabled = properties.thinkingEnabled();
        this.objectMapper = new ObjectMapper();
        Duration connectTimeout = properties.connectTimeout();
        if (connectTimeout == null
                || connectTimeout.isZero()
                || connectTimeout.isNegative()) {
            throw configuration("DeepSeek 连接超时时间必须大于0");
        }
        Duration readTimeout = properties.timeout();
        if (readTimeout == null
                || readTimeout.isZero()
                || readTimeout.isNegative()) {
            throw configuration("DeepSeek 超时时间必须大于0");
        }
        builder.baseUrl(stripTrailingSlash(baseUrl));
        if (configureTimeout) {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(connectTimeout)
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            JdkClientHttpRequestFactory requestFactory =
                    new JdkClientHttpRequestFactory(httpClient);
            requestFactory.setReadTimeout(readTimeout);
            builder.requestFactory(requestFactory);
        }
        this.restClient = builder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
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
        long startedAt = System.nanoTime();
        String traceId = traceId();
        int inputChars = request.messages().stream()
                .mapToInt(message -> message.content() == null
                        ? 0
                        : message.content().length())
                .sum();
        LOGGER.info(
                "AI_PROVIDER_START traceId={} provider={} model={} endpoint={} "
                        + "transport=jdk-http-client stream=false thinking={} "
                        + "messages={} inputChars={} "
                        + "maxTokens={} responseFormat={}",
                traceId,
                PROVIDER,
                model,
                endpoint,
                thinkingEnabled,
                request.messages().size(),
                inputChars,
                request.maxOutputTokens(),
                request.responseFormat()
        );
        DeepSeekRequest providerRequest = new DeepSeekRequest(
                model,
                request.messages().stream()
                        .map(message -> new ProviderMessage(
                                message.role().name().toLowerCase(Locale.ROOT),
                                message.content()
                        ))
                        .toList(),
                false,
                request.maxOutputTokens(),
                request.temperature(),
                request.responseFormat() == AiResponseFormat.JSON_OBJECT
                        ? new ProviderResponseFormat("json_object")
                        : null,
                new ProviderThinking(thinkingEnabled ? "enabled" : "disabled")
        );
        try {
            return restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(providerRequest)
                    .exchange((httpRequest, response) -> {
                        int status = response.getStatusCode().value();
                        LOGGER.info(
                                "AI_PROVIDER_HEADERS traceId={} provider={} "
                                        + "status={} elapsedMs={}",
                                traceId,
                                PROVIDER,
                                status,
                                elapsedMillis(startedAt)
                        );
                        if (response.getStatusCode().isError()) {
                            throw safeHttpStatus(status);
                        }
                        DeepSeekResponse providerResponse =
                                objectMapper.readValue(
                                        response.getBody(),
                                        DeepSeekResponse.class
                                );
                        return normalize(
                                providerResponse,
                                traceId,
                                startedAt
                        );
                    });
        } catch (RestClientResponseException exception) {
            logFailure(traceId, startedAt, exception.getClass(), exception);
            throw safeHttpException(exception);
        } catch (ResourceAccessException exception) {
            logFailure(traceId, startedAt, AiClientException.Kind.TIMEOUT, exception);
            throw new AiClientException(
                    AiClientException.Kind.TIMEOUT,
                    "DeepSeek 调用超时或暂时不可用",
                    exception
            );
        } catch (AiClientException exception) {
            logFailure(traceId, startedAt, exception.getKind(), exception);
            throw exception;
        } catch (RuntimeException exception) {
            logFailure(
                    traceId,
                    startedAt,
                    AiClientException.Kind.PROVIDER_ERROR,
                    exception
            );
            throw new AiClientException(
                    AiClientException.Kind.PROVIDER_ERROR,
                    "DeepSeek 调用失败",
                    exception
            );
        }
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private AiClientResponse normalize(
            DeepSeekResponse response,
            String traceId,
            long startedAt
    ) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0) == null
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null
                || response.choices().get(0).message().content().isBlank()) {
            throw new AiClientException(
                    AiClientException.Kind.INVALID_RESPONSE,
                    "DeepSeek 返回内容为空"
            );
        }
        Choice choice = response.choices().get(0);
        String content = choice.message().content().trim();
        Usage normalizedUsage = response.usage() == null
                ? new Usage(0, 0, 0)
                : response.usage();
        LOGGER.info(
                "AI_PROVIDER_SUCCESS traceId={} provider={} requestId={} "
                        + "elapsedMs={} outputChars={} finishReason={} totalTokens={}",
                traceId,
                PROVIDER,
                response.id() == null ? "-" : response.id(),
                elapsedMillis(startedAt),
                content.length(),
                choice.finishReason() == null ? "-" : choice.finishReason(),
                nonNegative(normalizedUsage.totalTokens())
        );
        return new AiClientResponse(
                PROVIDER,
                response.model() == null ? model : response.model(),
                response.id(),
                content,
                choice.finishReason(),
                nonNegative(normalizedUsage.promptTokens()),
                nonNegative(normalizedUsage.completionTokens()),
                nonNegative(normalizedUsage.totalTokens())
        );
    }

    /** 执行 logFailure 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private void logFailure(
            String traceId,
            long startedAt,
            Object failureKind,
            Throwable exception
    ) {
        LOGGER.warn(
                "AI_PROVIDER_FAILURE traceId={} provider={} model={} endpoint={} "
                        + "elapsedMs={} kind={} cause={}",
                traceId,
                PROVIDER,
                model,
                endpoint,
                elapsedMillis(startedAt),
                failureKind,
                safeCause(exception)
        );
    }

    /** 执行 safeCause 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String safeCause(Throwable exception) {
        Throwable current = exception;
        int depth = 0;
        while (current.getCause() != null && depth++ < 8) {
            current = current.getCause();
        }
        String message = current.getMessage();
        String normalized = message == null
                ? ""
                : message.replaceAll("[\\r\\n\\t]+", " ").trim();
        if (normalized.length() > 300) {
            normalized = normalized.substring(0, 300);
        }
        return current.getClass().getSimpleName()
                + (normalized.isEmpty() ? "" : ": " + normalized);
    }

    /** 执行 elapsedMillis 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    /** 执行 traceId 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "-" : value;
    }

    /** 执行 safeHttpException 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AiClientException safeHttpException(RestClientResponseException exception) {
        return safeHttpStatus(exception.getStatusCode().value(), exception);
    }

    /** 执行 safeHttpStatus 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AiClientException safeHttpStatus(int status) {
        return safeHttpStatus(status, null);
    }

    /** 执行 safeHttpStatus 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AiClientException safeHttpStatus(int status, Throwable cause) {
        AiClientException.Kind kind;
        if (status == 401 || status == 403) {
            kind = AiClientException.Kind.AUTHENTICATION;
        } else if (status == 429) {
            kind = AiClientException.Kind.RATE_LIMIT;
        } else {
            kind = AiClientException.Kind.PROVIDER_ERROR;
        }
        return new AiClientException(
                kind,
                "DeepSeek 请求失败（HTTP " + status + "）",
                cause
        );
    }

    /** 执行 nonNegative 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    /** 执行 stripTrailingSlash 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** 校验Text及相关业务前置条件，不满足时抛出明确业务异常。 */
    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw configuration(message);
        }
        return value.trim();
    }

    /** 执行 configuration 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AiClientException configuration(String message) {
        return new AiClientException(AiClientException.Kind.CONFIGURATION, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    /** 执行 DeepSeekRequest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record DeepSeekRequest(
            String model,
            List<ProviderMessage> messages,
            boolean stream,
            @JsonProperty("max_tokens") Integer maxTokens,
            Double temperature,
            @JsonProperty("response_format")
            ProviderResponseFormat responseFormat,
            ProviderThinking thinking
    ) {
    }

    /** 执行 ProviderResponseFormat 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record ProviderResponseFormat(String type) {
    }

    /** 执行 ProviderThinking 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record ProviderThinking(String type) {
    }

    /** 执行 ProviderMessage 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record ProviderMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** 执行 DeepSeekResponse 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record DeepSeekResponse(
            String id,
            String model,
            List<Choice> choices,
            Usage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** 执行 Choice 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record Choice(
            ProviderMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    /** 执行 Usage 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
