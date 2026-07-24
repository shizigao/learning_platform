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

public class DeepSeekAiClient implements AiClient {
    private static final String PROVIDER = "deepseek";
    private static final Logger LOGGER = LoggerFactory.getLogger(DeepSeekAiClient.class);
    private final String model;
    private final String endpoint;
    private final boolean thinkingEnabled;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public DeepSeekAiClient(AiProperties.DeepSeek properties) {
        this(properties, RestClient.builder(), true);
    }

    DeepSeekAiClient(
            AiProperties.DeepSeek properties,
            RestClient.Builder builder
    ) {
        this(properties, builder, false);
    }

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
    public String provider() {
        return PROVIDER;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
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

    private long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private String traceId() {
        String value = MDC.get("traceId");
        return value == null || value.isBlank() ? "-" : value;
    }

    private AiClientException safeHttpException(RestClientResponseException exception) {
        return safeHttpStatus(exception.getStatusCode().value(), exception);
    }

    private AiClientException safeHttpStatus(int status) {
        return safeHttpStatus(status, null);
    }

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

    private int nonNegative(Integer value) {
        return value == null ? 0 : Math.max(value, 0);
    }

    private String stripTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw configuration(message);
        }
        return value.trim();
    }

    private AiClientException configuration(String message) {
        return new AiClientException(AiClientException.Kind.CONFIGURATION, message);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    private record ProviderResponseFormat(String type) {
    }

    private record ProviderThinking(String type) {
    }

    private record ProviderMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekResponse(
            String id,
            String model,
            List<Choice> choices,
            Usage usage
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(
            ProviderMessage message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(
            @JsonProperty("prompt_tokens") Integer promptTokens,
            @JsonProperty("completion_tokens") Integer completionTokens,
            @JsonProperty("total_tokens") Integer totalTokens
    ) {
    }
}
