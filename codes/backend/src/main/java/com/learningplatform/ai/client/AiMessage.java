/* 文件职责：以不可变记录表示AI消息数据，并作为模块内部或接口层的数据契约。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

/**
 * 以不可变记录表示AI消息数据，并作为模块内部或接口层的数据契约。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
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
