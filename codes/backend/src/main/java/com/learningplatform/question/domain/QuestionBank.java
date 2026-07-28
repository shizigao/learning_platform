/* 文件职责：表示题目题库领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：题库、题目、选项与标准答案；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.question.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示题目题库领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class QuestionBank extends BaseEntity {
    /** 保存ownerID，供该类型的业务逻辑读取或更新。 */
    private Long ownerId;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存description，供该类型的业务逻辑读取或更新。 */
    private String description;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private QuestionStatus status;

    /** 返回OwnerID。 */
    public Long getOwnerId() {
        return ownerId;
    }

    /** 更新OwnerID；调用方仍需遵守所属领域的校验规则。 */
    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    /** 返回名称。 */
    public String getName() {
        return name;
    }

    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回Description。 */
    public String getDescription() {
        return description;
    }

    /** 更新Description；调用方仍需遵守所属领域的校验规则。 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 返回状态。 */
    public QuestionStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(QuestionStatus status) {
        this.status = status;
    }
}
