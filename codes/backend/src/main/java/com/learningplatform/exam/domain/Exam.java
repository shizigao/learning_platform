package com.learningplatform.exam.domain;

import com.learningplatform.common.model.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Exam extends BaseEntity {
    private Long publisherId;
    private Long paperId;
    private String name;
    private String instructions;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer durationMinutes;
    private BigDecimal passingScore;
    private Boolean showResultImmediately;
    private Boolean showAnswerAfterFinish;
    private ExamStatus status;
    private LocalDateTime publishedAt;
    private LocalDateTime finishedAt;
    private Integer version;

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public BigDecimal getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(BigDecimal passingScore) {
        this.passingScore = passingScore;
    }

    public Boolean getShowResultImmediately() {
        return showResultImmediately;
    }

    public void setShowResultImmediately(Boolean showResultImmediately) {
        this.showResultImmediately = showResultImmediately;
    }

    public Boolean getShowAnswerAfterFinish() {
        return showAnswerAfterFinish;
    }

    public void setShowAnswerAfterFinish(Boolean showAnswerAfterFinish) {
        this.showAnswerAfterFinish = showAnswerAfterFinish;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
