package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Size;

public class ContentCategorySearchQuery extends PageQuery {
    @Size(max = 100, message = "分类搜索关键词不能超过100个字符")
    private String keyword;

    public ContentCategorySearchQuery() {
        setPageSize(20);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
