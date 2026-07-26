package com.learningplatform.exam.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Size;

public class ExamCandidateSearchQuery extends PageQuery {
    @Size(max = 64, message = "考生搜索关键字不能超过64个字符")
    private String keyword;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
