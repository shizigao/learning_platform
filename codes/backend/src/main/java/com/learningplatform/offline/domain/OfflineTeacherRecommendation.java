/* 文件职责：表示线下教学教师推荐领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.offline.domain;

import java.time.LocalDateTime;

/**
 * 表示线下教学教师推荐领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class OfflineTeacherRecommendation {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存任务ID，供该类型的业务逻辑读取或更新。 */
    private Long taskId;
    /** 保存用户ID，供该类型的业务逻辑读取或更新。 */
    private Long userId;
    /** 保存学习需求Snapshot，供该类型的业务逻辑读取或更新。 */
    private String preferenceSnapshot;
    /** 保存考生Snapshot，供该类型的业务逻辑读取或更新。 */
    private String candidateSnapshot;
    /** 保存推荐Json，供该类型的业务逻辑读取或更新。 */
    private String recommendationJson;
    /** 保存输入SnapshotHash，供该类型的业务逻辑读取或更新。 */
    private String inputSnapshotHash;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;

    /** 返回ID。 */
    public Long getId() { return id; }
    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long value) { id = value; }
    /** 返回任务ID。 */
    public Long getTaskId() { return taskId; }
    /** 更新任务ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTaskId(Long value) { taskId = value; }
    /** 返回用户ID。 */
    public Long getUserId() { return userId; }
    /** 更新用户ID；调用方仍需遵守所属领域的校验规则。 */
    public void setUserId(Long value) { userId = value; }
    /** 返回学习需求Snapshot。 */
    public String getPreferenceSnapshot() { return preferenceSnapshot; }
    /** 更新学习需求Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setPreferenceSnapshot(String value) { preferenceSnapshot = value; }
    /** 返回考生Snapshot。 */
    public String getCandidateSnapshot() { return candidateSnapshot; }
    /** 更新考生Snapshot；调用方仍需遵守所属领域的校验规则。 */
    public void setCandidateSnapshot(String value) { candidateSnapshot = value; }
    /** 返回推荐Json。 */
    public String getRecommendationJson() { return recommendationJson; }
    /** 更新推荐Json；调用方仍需遵守所属领域的校验规则。 */
    public void setRecommendationJson(String value) { recommendationJson = value; }
    /** 返回输入SnapshotHash。 */
    public String getInputSnapshotHash() { return inputSnapshotHash; }
    /** 更新输入SnapshotHash；调用方仍需遵守所属领域的校验规则。 */
    public void setInputSnapshotHash(String value) { inputSnapshotHash = value; }
    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() { return createdAt; }
    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
