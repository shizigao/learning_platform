/* 文件职责：枚举学习资料类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.domain;

/**
 * 枚举学习资料类型允许的状态或类型，是数据库值、业务分支和接口契约的共同边界。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public enum ContentType {
    GENERAL,
    // Legacy values remain readable until database/003_unify_content_type.sql is executed.
    ARTICLE,
    DOCUMENT,
    VIDEO,
    ATTACHMENT,
    MIXED
}
