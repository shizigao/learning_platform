package com.learningplatform.ai.client;

public record AiMessage(AiRole role, String content) {
    public AiMessage {
        if (role == null) {
            throw new IllegalArgumentException("AI 消息角色不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("AI 消息内容不能为空");
        }
        content = content.trim();
    }
}
