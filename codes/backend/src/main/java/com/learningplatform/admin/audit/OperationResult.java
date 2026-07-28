/* 文件职责：枚举操作成绩允许的有限取值，供持久化、校验和状态分支共同使用。
 * 所属模块：平台治理与管理员操作；所在分层：审计基础设施层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.audit;

/**
 * 枚举操作成绩允许的有限取值，供持久化、校验和状态分支共同使用。
 *
 * <p>职责边界：遵守 平台治理与管理员操作 模块的职责边界。</p>
 */
public enum OperationResult {
    SUCCESS,
    FAILURE
}
