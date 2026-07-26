package com.learningplatform.content.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class ContentReferenceSearchQuery extends PageQuery {
    @Size(max = 100, message = "资料名称搜索关键词不能超过100个字符")
    private String titleKeyword;

    @Size(max = 100, message = "发布者名字搜索关键词不能超过100个字符")
    private String publisherKeyword;

    @Min(value = 1, message = "排除的资料ID必须为正数")
    private Long excludeContentId;

    public String getTitleKeyword() {
        return titleKeyword;
    }

    public void setTitleKeyword(String titleKeyword) {
        this.titleKeyword = titleKeyword;
    }

    public String getPublisherKeyword() {
        return publisherKeyword;
    }

    public void setPublisherKeyword(String publisherKeyword) {
        this.publisherKeyword = publisherKeyword;
    }

    public Long getExcludeContentId() {
        return excludeContentId;
    }

    public void setExcludeContentId(Long excludeContentId) {
        this.excludeContentId = excludeContentId;
    }
}
