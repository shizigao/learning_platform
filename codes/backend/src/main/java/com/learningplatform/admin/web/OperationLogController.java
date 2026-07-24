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
public class OperationLogController {
    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public ApiResponse<PageResult<OperationLogResponse>> list(
            @Valid @ModelAttribute OperationLogListQuery query
    ) {
        return ApiResponse.success(operationLogService.list(query));
    }
}
