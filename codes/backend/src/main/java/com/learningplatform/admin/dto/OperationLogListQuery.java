package com.learningplatform.admin.dto;

import com.learningplatform.admin.audit.OperationResult;
import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class OperationLogListQuery extends PageQuery {
    @Positive(message = "操作人ID必须大于0")
    private Long operatorId;
    @Size(max = 64, message = "模块名不能超过64个字符")
    private String module;
    @Size(max = 64, message = "操作名不能超过64个字符")
    private String action;
    private OperationResult result;
    @Size(max = 64, message = "请求ID不能超过64个字符")
    private String requestId;

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
