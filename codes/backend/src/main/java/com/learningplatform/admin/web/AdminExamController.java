package com.learningplatform.admin.web;

import com.learningplatform.admin.dto.AdminExamDetailResponse;
import com.learningplatform.admin.dto.AdminExamListQuery;
import com.learningplatform.admin.dto.AdminExamSummaryResponse;
import com.learningplatform.admin.service.AdminExamService;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/exams")
public class AdminExamController {
    private final AdminExamService adminExamService;

    public AdminExamController(AdminExamService adminExamService) {
        this.adminExamService = adminExamService;
    }

    @GetMapping
    public ApiResponse<PageResult<AdminExamSummaryResponse>> list(
            @Valid @ModelAttribute AdminExamListQuery query
    ) {
        return ApiResponse.success(adminExamService.list(query));
    }

    @GetMapping("/{examId}")
    public ApiResponse<AdminExamDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        return ApiResponse.success(adminExamService.detail(
                AuthenticationPrincipalResolver.require(authentication).userId(),
                examId
        ));
    }
}
