package com.learningplatform.user.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Size;

public class UserSearchQuery extends PageQuery {
    @Size(max = 64, message = "用户名搜索关键词不能超过64个字符")
    private String keyword;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
