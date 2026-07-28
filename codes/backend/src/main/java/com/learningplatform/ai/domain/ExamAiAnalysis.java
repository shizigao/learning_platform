/* 文件职责：表示考试AI分析领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：AI 任务、对话、分析与供应商调用；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.ai.domain;

import com.learningplatform.common.model.BaseEntity;

/**
 * 表示考试AI分析领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class ExamAiAnalysis extends BaseEntity {
    /** 保存任务ID，供该类型的业务逻辑读取或更新。 */
    private Long taskId;
    /** 保存考试ID，供该类型的业务逻辑读取或更新。 */
    private Long examId;
    /** 保存作答ID，供该类型的业务逻辑读取或更新。 */
    private Long attemptId;
    /** 保存requesterID，供该类型的业务逻辑读取或更新。 */
    private Long requesterId;
    /** 保存分析范围，供该类型的业务逻辑读取或更新。 */
    private ExamAiAnalysisScope analysisScope;
    /** 保存reportMarkdown，供该类型的业务逻辑读取或更新。 */
    private String reportMarkdown;
    /** 保存输入SnapshotHash，供该类型的业务逻辑读取或更新。 */
    private String inputSnapshotHash;

    /** 返回任务ID。 */
    public Long getTaskId() { return taskId; }
    /** 更新任务ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    /** 返回考试ID。 */
    public Long getExamId() { return examId; }
    /** 更新考试ID；调用方仍需遵守所属领域的校验规则。 */
    public void setExamId(Long examId) { this.examId = examId; }
    /** 返回作答ID。 */
    public Long getAttemptId() { return attemptId; }
    /** 更新作答ID；调用方仍需遵守所属领域的校验规则。 */
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    /** 返回RequesterID。 */
    public Long getRequesterId() { return requesterId; }
    /** 更新RequesterID；调用方仍需遵守所属领域的校验规则。 */
    public void setRequesterId(Long requesterId) { this.requesterId = requesterId; }
    /** 返回分析范围。 */
    public ExamAiAnalysisScope getAnalysisScope() { return analysisScope; }
    /** 更新分析范围；调用方仍需遵守所属领域的校验规则。 */
    public void setAnalysisScope(ExamAiAnalysisScope analysisScope) {
        this.analysisScope = analysisScope;
    }
    /** 返回ReportMarkdown。 */
    public String getReportMarkdown() { return reportMarkdown; }
    /** 更新ReportMarkdown；调用方仍需遵守所属领域的校验规则。 */
    public void setReportMarkdown(String reportMarkdown) {
        this.reportMarkdown = reportMarkdown;
    }
    /** 返回输入SnapshotHash。 */
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    /** 更新输入SnapshotHash；调用方仍需遵守所属领域的校验规则。 */
    public void setInputSnapshotHash(String inputSnapshotHash) {
        this.inputSnapshotHash = inputSnapshotHash;
    }
}
