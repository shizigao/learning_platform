/* 文件职责：枚举AI响应Format允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：外部服务适配层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.client;

/**
 * 枚举AI响应Format允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 *
 * <p>职责边界：负责协议转换、超时和安全日志，不直接扣减业务权益。</p>
 */
public enum AiResponseFormat {
    TEXT,
    JSON_OBJECT
}
