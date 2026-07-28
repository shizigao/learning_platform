/* 文件职责：定义或实现AI 客户端外部调用适配，隔离供应商协议与业务服务。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

/**
 * 定义或实现AI 客户端外部调用适配，隔离供应商协议与业务服务。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public interface AiClient {
    /** 返回当前 AI 供应商标识，用于任务记录、用量审计和运行日志。 */
    String provider();

    /** 返回当前 AI 模型标识，用于任务快照和问题追踪。 */
    String model();

    /** 发送一次对话补全请求并返回供应商无关的规范化结果；失败时抛出 AI 客户端异常。 */
    AiClientResponse complete(AiClientRequest request);
}
