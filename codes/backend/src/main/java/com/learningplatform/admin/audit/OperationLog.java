/* 文件职责：表示操作日志领域对象或组件，封装该概念相关的数据和行为。
 * 所属模块：平台治理与管理员操作；所在分层：审计基础设施层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.audit;

import java.time.LocalDateTime;

/**
 * 表示操作日志领域对象或组件，封装该概念相关的数据和行为。
 *
 * <p>职责边界：遵守 平台治理与管理员操作 模块的职责边界。</p>
 */
public class OperationLog {
    /** 保存ID，供该类型的业务逻辑读取或更新。 */
    private Long id;
    /** 保存operatorID，供该类型的业务逻辑读取或更新。 */
    private Long operatorId;
    /** 保存operator名称，供该类型的业务逻辑读取或更新。 */
    private String operatorName;
    /** 保存module，供该类型的业务逻辑读取或更新。 */
    private String module;
    /** 保存action，供该类型的业务逻辑读取或更新。 */
    private String action;
    /** 保存目标类型，供该类型的业务逻辑读取或更新。 */
    private String targetType;
    /** 保存目标ID，供该类型的业务逻辑读取或更新。 */
    private String targetId;
    /** 保存请求Method，供该类型的业务逻辑读取或更新。 */
    private String requestMethod;
    /** 保存请求Path，供该类型的业务逻辑读取或更新。 */
    private String requestPath;
    /** 保存请求ID，供该类型的业务逻辑读取或更新。 */
    private String requestId;
    /** 保存ipAddress，供该类型的业务逻辑读取或更新。 */
    private String ipAddress;
    /** 保存用户Agent，供该类型的业务逻辑读取或更新。 */
    private String userAgent;
    /** 保存成绩，供该类型的业务逻辑读取或更新。 */
    private OperationResult result;
    /** 保存详情Json，供该类型的业务逻辑读取或更新。 */
    private String detailJson;
    /** 保存错误消息，供该类型的业务逻辑读取或更新。 */
    private String errorMessage;
    /** 保存durationMs，供该类型的业务逻辑读取或更新。 */
    private Integer durationMs;
    /** 保存创建时间，供该类型的业务逻辑读取或更新。 */
    private LocalDateTime createdAt;

    /** 返回ID。 */
    public Long getId() {
        return id;
    }

    /** 更新ID；调用方仍需遵守所属领域的校验规则。 */
    public void setId(Long id) {
        this.id = id;
    }

    /** 返回OperatorID。 */
    public Long getOperatorId() {
        return operatorId;
    }

    /** 更新OperatorID；调用方仍需遵守所属领域的校验规则。 */
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    /** 返回Operator名称。 */
    public String getOperatorName() {
        return operatorName;
    }

    /** 更新Operator名称；调用方仍需遵守所属领域的校验规则。 */
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
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

    /** 返回目标类型。 */
    public String getTargetType() {
        return targetType;
    }

    /** 更新目标类型；调用方仍需遵守所属领域的校验规则。 */
    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    /** 返回目标ID。 */
    public String getTargetId() {
        return targetId;
    }

    /** 更新目标ID；调用方仍需遵守所属领域的校验规则。 */
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /** 返回请求Method。 */
    public String getRequestMethod() {
        return requestMethod;
    }

    /** 更新请求Method；调用方仍需遵守所属领域的校验规则。 */
    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    /** 返回请求Path。 */
    public String getRequestPath() {
        return requestPath;
    }

    /** 更新请求Path；调用方仍需遵守所属领域的校验规则。 */
    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    /** 返回请求ID。 */
    public String getRequestId() {
        return requestId;
    }

    /** 更新请求ID；调用方仍需遵守所属领域的校验规则。 */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /** 返回IpAddress。 */
    public String getIpAddress() {
        return ipAddress;
    }

    /** 更新IpAddress；调用方仍需遵守所属领域的校验规则。 */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /** 返回用户Agent。 */
    public String getUserAgent() {
        return userAgent;
    }

    /** 更新用户Agent；调用方仍需遵守所属领域的校验规则。 */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /** 返回成绩。 */
    public OperationResult getResult() {
        return result;
    }

    /** 更新成绩；调用方仍需遵守所属领域的校验规则。 */
    public void setResult(OperationResult result) {
        this.result = result;
    }

    /** 返回详情Json。 */
    public String getDetailJson() {
        return detailJson;
    }

    /** 更新详情Json；调用方仍需遵守所属领域的校验规则。 */
    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    /** 返回错误消息。 */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** 更新错误消息；调用方仍需遵守所属领域的校验规则。 */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** 返回DurationMs。 */
    public Integer getDurationMs() {
        return durationMs;
    }

    /** 更新DurationMs；调用方仍需遵守所属领域的校验规则。 */
    public void setDurationMs(Integer durationMs) {
        this.durationMs = durationMs;
    }

    /** 返回创建时间。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 更新创建时间；调用方仍需遵守所属领域的校验规则。 */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
