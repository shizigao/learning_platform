package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.dto.ExamListQuery;
import com.learningplatform.exam.dto.ExamManagementResponse;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.dto.ExamWriteRequest;
import com.learningplatform.exam.service.ExamService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/publisher/exams")
public class PublisherExamController {
    private final ExamService examService;

    public PublisherExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public ApiResponse<PageResult<ExamSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute ExamListQuery query
    ) {
        return ApiResponse.success(examService.list(principal(authentication).userId(), query));
    }

    @PostMapping
    public ApiResponse<ExamManagementResponse> create(
            Authentication authentication,
            @Valid @RequestBody ExamWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.create(
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @GetMapping("/{examId}")
    public ApiResponse<ExamManagementResponse> detail(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.detail(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/{examId}")
    public ApiResponse<ExamManagementResponse> update(
            Authentication authentication,
            @PathVariable Long examId,
            @Valid @RequestBody ExamWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.update(
                examId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @PostMapping("/{examId}/publish")
    public ApiResponse<ExamManagementResponse> publish(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.publish(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/{examId}/cancel")
    public ApiResponse<ExamManagementResponse> cancel(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(examService.cancel(
                examId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/{examId}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long examId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        examService.delete(examId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @GetMapping("/quota")
    public ApiResponse<Integer> quota(Authentication authentication) {
        return ApiResponse.success(examService.availableQuota(principal(authentication).userId()));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
