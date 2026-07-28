/* 文件职责：枚举教师申请状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.domain;

/**
 * 枚举教师申请状态允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public enum TeacherApplicationStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    WITHDRAWN
}
