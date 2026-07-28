/* 文件职责：提供操作日志相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：平台治理与管理员操作；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.admin.web;

import com.learningplatform.admin.audit.OperationLogService;
import com.learningplatform.admin.dto.OperationLogListQuery;
import com.learningplatform.admin.dto.OperationLogResponse;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/operation-logs")
/**
 * 提供操作日志相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class OperationLogController {
    /** 委托操作日志执行对应领域规则。 */
    private final OperationLogService operationLogService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<OperationLogResponse>> list(
            @Valid @ModelAttribute OperationLogListQuery query
    ) {
        return ApiResponse.success(operationLogService.list(query));
    }
}
