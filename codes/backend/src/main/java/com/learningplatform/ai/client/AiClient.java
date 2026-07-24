package com.learningplatform.ai.client;

public interface AiClient {
    String provider();

    String model();

    AiClientResponse complete(AiClientRequest request);
}
