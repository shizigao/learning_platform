/* 文件职责：表示考试领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：领域模型层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 表示考试领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：保存领域状态，不依赖 Web 层，也不负责发起外部调用。</p>
 */
public class Exam extends BaseEntity {
    /** 保存发布者ID，供该类型的业务逻辑读取或更新。 */
    private Long publisherId;
    /** 保存试卷ID，供该类型的业务逻辑读取或更新。 */
    private Long paperId;
    /** 保存名称，供该类型的业务逻辑读取或更新。 */
    private String name;
    /** 保存instructions，供该类型的业务逻辑读取或更新。 */
    private String instructions;
    /** 保存assignment模式，供该类型的业务逻辑读取或更新。 */
    private ExamAssignmentMode assignmentMode;
    /** 保存开始时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime startAt;
    /** 保存end时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime endAt;
    /** 保存durationMinutes，供该类型的业务逻辑读取或更新。 */
    private Integer durationMinutes;
    /** 保存passing分数，供该类型的业务逻辑读取或更新。 */
    private BigDecimal passingScore;
    /** 保存show成绩Immediately，供该类型的业务逻辑读取或更新。 */
    private Boolean showResultImmediately;
    /** 保存show答案After结束，供该类型的业务逻辑读取或更新。 */
    private Boolean showAnswerAfterFinish;
    /** 保存状态，供该类型的业务逻辑读取或更新。 */
    private ExamStatus status;
    /** 保存发布时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime publishedAt;
    /** 保存finished时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime finishedAt;
    /** 保存version，供该类型的业务逻辑读取或更新。 */
    private Integer version;

    /** 返回发布者ID。 */
    public Long getPublisherId() {
        return publisherId;
    }

    /** 更新发布者ID；调用方仍需遵守所属领域的校验规则。 */
    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    /** 返回试卷ID。 */
    public Long getPaperId() {
        return paperId;
    }

    /** 更新试卷ID；调用方仍需遵守所属领域的校验规则。 */
    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    /** 返回名称。 */
    public String getName() {
        return name;
    }

    /** 更新名称；调用方仍需遵守所属领域的校验规则。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回Instructions。 */
    public String getInstructions() {
        return instructions;
    }

    /** 更新Instructions；调用方仍需遵守所属领域的校验规则。 */
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    /** 返回Assignment模式。 */
    public ExamAssignmentMode getAssignmentMode() {
        return assignmentMode;
    }

    /** 更新Assignment模式；调用方仍需遵守所属领域的校验规则。 */
    public void setAssignmentMode(ExamAssignmentMode assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    /** 返回开始时间。 */
    public LocalDateTime getStartAt() {
        return startAt;
    }

    /** 更新开始时间；调用方仍需遵守所属领域的校验规则。 */
    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    /** 返回End时间。 */
    public LocalDateTime getEndAt() {
        return endAt;
    }

    /** 更新End时间；调用方仍需遵守所属领域的校验规则。 */
    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    /** 返回DurationMinutes。 */
    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    /** 更新DurationMinutes；调用方仍需遵守所属领域的校验规则。 */
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    /** 返回Passing分数。 */
    public BigDecimal getPassingScore() {
        return passingScore;
    }

    /** 更新Passing分数；调用方仍需遵守所属领域的校验规则。 */
    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    /** 返回Show成绩Immediately。 */
    public Boolean getShowResultImmediately() {
        return showResultImmediately;
    }

    /** 更新Show成绩Immediately；调用方仍需遵守所属领域的校验规则。 */
    public void setShowResultImmediately(Boolean showResultImmediately) {
        this.showResultImmediately = showResultImmediately;
    }

    /** 返回Show答案After结束。 */
    public Boolean getShowAnswerAfterFinish() {
        return showAnswerAfterFinish;
    }

    /** 更新Show答案After结束；调用方仍需遵守所属领域的校验规则。 */
    public void setShowAnswerAfterFinish(Boolean showAnswerAfterFinish) {
        this.showAnswerAfterFinish = showAnswerAfterFinish;
    }

    /** 返回状态。 */
    public ExamStatus getStatus() {
        return status;
    }

    /** 更新状态；调用方仍需遵守所属领域的校验规则。 */
    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    /** 返回发布时间。 */
    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    /** 更新发布时间；调用方仍需遵守所属领域的校验规则。 */
    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    /** 返回Finished时间。 */
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    /** 更新Finished时间；调用方仍需遵守所属领域的校验规则。 */
    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    /** 返回Version。 */
    public Integer getVersion() {
        return version;
    }

    /** 更新Version；调用方仍需遵守所属领域的校验规则。 */
    public void setVersion(Integer version) {
        this.version = version;
    }
}
