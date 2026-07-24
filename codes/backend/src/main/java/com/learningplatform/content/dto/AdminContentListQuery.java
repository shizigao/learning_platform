package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.content.domain.ContentStatus;
import jakarta.validation.constraints.Size;

public class AdminContentListQuery extends PageQuery {
    private ContentStatus status;

    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    private String keyword;

    public ContentStatus getStatus() {
        return status;
    }

    public void setStatus(ContentStatus status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
