/* 文件职责：表示AI总结领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示AI总结领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class AiSummary extends BaseEntity {
    /** 保存任务ID，供该类型的业务逻辑读取或更新。 */
    private Long taskId;
    /** 保存学习资料ID，供该类型的业务逻辑读取或更新。 */
    private Long contentId;
    /** 保存总结Text，供该类型的业务逻辑读取或更新。 */
    private String summaryText;
    /** 保存knowledgePointsJson，供该类型的业务逻辑读取或更新。 */
    private String knowledgePointsJson;
    /** 保存复习Outline，供该类型的业务逻辑读取或更新。 */
    private String reviewOutline;
    /** 保存来源Version，供该类型的业务逻辑读取或更新。 */
    private String sourceVersion;

    /** 返回任务ID。 */
    public Long getTaskId() { return taskId; }
    /** 更新任务ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    /** 返回学习资料ID。 */
    public Long getContentId() { return contentId; }
    /** 更新学习资料ID；调用方仍需遵守所属领域的校验规则。 */
    public void setContentId(Long contentId) { this.contentId = contentId; }
    /** 返回总结Text。 */
    public String getSummaryText() { return summaryText; }
    /** 更新总结Text；调用方仍需遵守所属领域的校验规则。 */
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    /** 返回KnowledgePointsJson。 */
    public String getKnowledgePointsJson() { return knowledgePointsJson; }
    /** 更新KnowledgePointsJson；调用方仍需遵守所属领域的校验规则。 */
    public void setKnowledgePointsJson(String knowledgePointsJson) { this.knowledgePointsJson = knowledgePointsJson; }
    /** 返回复习Outline。 */
    public String getReviewOutline() { return reviewOutline; }
    /** 更新复习Outline；调用方仍需遵守所属领域的校验规则。 */
    public void setReviewOutline(String reviewOutline) { this.reviewOutline = reviewOutline; }
    /** 返回来源Version。 */
    public String getSourceVersion() { return sourceVersion; }
    /** 更新来源Version；调用方仍需遵守所属领域的校验规则。 */
    public void setSourceVersion(String sourceVersion) { this.sourceVersion = sourceVersion; }
}
