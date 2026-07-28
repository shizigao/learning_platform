/* 文件职责：定义操作日志列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 * 所属模块：平台治理与管理员操作；所在分层：接口数据契约层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.dto;

import com.learningplatform.admin.audit.OperationResult;
import com.learningplatform.common.page.PageQuery;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 定义操作日志列表查询条件列表或检索接口的查询条件、分页参数和默认值。
 *
 * <p>职责边界：字段与 JSON 契约保持一致，不承载数据库连接或外部副作用。</p>
 */
public class OperationLogListQuery extends PageQuery {
    @Positive(message = "操作人ID必须大于0")
    /** 保存operatorID，供该类型的业务逻辑读取或更新。 */
    private Long operatorId;
    @Size(max = 64, message = "模块名不能超过64个字符")
    /** 保存module，供该类型的业务逻辑读取或更新。 */
    private String module;
    @Size(max = 64, message = "操作名不能超过64个字符")
    /** 保存action，供该类型的业务逻辑读取或更新。 */
    private String action;
    /** 保存成绩，供该类型的业务逻辑读取或更新。 */
    private OperationResult result;
    @Size(max = 64, message = "请求ID不能超过64个字符")
    /** 保存请求ID，供该类型的业务逻辑读取或更新。 */
    private String requestId;

    /** 返回OperatorID。 */
    public Long getOperatorId() {
        return operatorId;
    }

    /** 更新OperatorID；调用方仍需遵守所属领域的校验规则。 */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /** 返回Module。 */
    public String getModule() {
        return module;
    }

    /** 更新Module；调用方仍需遵守所属领域的校验规则。 */
    public void setModule(String module) {
        this.module = module;
    }

    /** 返回Action。 */
    public String getAction() {
        return action;
    }

    /** 更新Action；调用方仍需遵守所属领域的校验规则。 */
    public void setAction(String action) {
        this.action = action;
    }

    /** 返回成绩。 */
    public OperationResult getResult() {
        return result;
    }

    /** 更新成绩；调用方仍需遵守所属领域的校验规则。 */
    public void setResult(OperationResult result) {
        this.result = result;
    }

    /** 返回请求ID。 */
    public String getRequestId() {
        return requestId;
    }

    /** 更新请求ID；调用方仍需遵守所属领域的校验规则。 */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
