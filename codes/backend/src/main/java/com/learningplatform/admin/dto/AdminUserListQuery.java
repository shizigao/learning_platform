package com.learningplatform.admin.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.UserStatus;
import jakarta.validation.constraints.Size;

public class AdminUserListQuery extends PageQuery {
    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;
    private UserStatus status;
    private RoleCode role;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public RoleCode getRole() {
        return role;
    }

    public void setRole(RoleCode role) {
        this.role = role;
    }
}
