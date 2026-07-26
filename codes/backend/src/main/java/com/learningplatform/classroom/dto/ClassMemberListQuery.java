package com.learningplatform.classroom.dto;

import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Size;

public class ClassMemberListQuery extends PageQuery {
    @Size(max = 100, message = "成员搜索关键字不能超过100个字符")
    private String keyword;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
}
