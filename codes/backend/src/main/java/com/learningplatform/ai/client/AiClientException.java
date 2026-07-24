package com.learningplatform.ai.client;

public class AiClientException extends RuntimeException {
    private final Kind kind;

    public AiClientException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public AiClientException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    public enum Kind {
        CONFIGURATION,
        AUTHENTICATION,
        RATE_LIMIT,
        TIMEOUT,
        PROVIDER_ERROR,
        INVALID_RESPONSE
    }
}
