package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.content.domain.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ContentListQuery extends PageQuery {
    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    private String keyword;

    @Min(value = 1, message = "分类ID必须为正数")
    private Long categoryId;

    private ContentType contentType;
    private Boolean free;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
    }

    public Boolean getFree() {
        return free;
    }

    public void setFree(Boolean free) {
        this.free = free;
    }
}
