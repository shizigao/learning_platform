package com.learningplatform.admin.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.exam.domain.ExamStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AdminExamListQuery extends PageQuery {
    @Positive(message = "发布者ID必须大于0")
    private Long publisherId;
    private ExamStatus status;
    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;

    public Long getPublisherId() {
        return publisherId;
    }

    public void setPublisherId(Long publisherId) {
        this.publisherId = publisherId;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void setStatus(ExamStatus status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
