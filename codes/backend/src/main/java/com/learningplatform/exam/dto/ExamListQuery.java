package com.learningplatform.exam.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.exam.domain.ExamStatus;
import jakarta.validation.constraints.Size;

public class ExamListQuery extends PageQuery {
    private ExamStatus status;

    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;

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
