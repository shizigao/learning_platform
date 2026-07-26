package com.learningplatform.offline.dto;

import com.learningplatform.common.page.PageQuery;
import com.learningplatform.offline.domain.TeacherApplicationStatus;
import jakarta.validation.constraints.Size;

public class TeacherApplicationAdminQuery extends PageQuery {
    @Size(max = 100)
    private String keyword;
    private TeacherApplicationStatus status;

    public String getKeyword() { return keyword; }
    public void setKeyword(String value) {
        keyword = value == null || value.isBlank() ? null : value.trim();
    }
    public TeacherApplicationStatus getStatus() { return status; }
    public void setStatus(TeacherApplicationStatus value) { status = value; }
}
