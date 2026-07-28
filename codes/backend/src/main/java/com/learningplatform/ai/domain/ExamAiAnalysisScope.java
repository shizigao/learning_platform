/* 文件职责：枚举考试AI分析范围允许的有限取值，供持久化、校验和状态分支共同使用。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

/**
 * 枚举考试AI分析范围允许的有限取值，供持久化、校验和状态分支共同使用。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public enum ExamAiAnalysisScope {
    OVERALL,
    PERSONAL
}
