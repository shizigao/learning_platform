package com.learningplatform.exam.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.exam.dto.ExamPaperDetailResponse;
import com.learningplatform.exam.dto.ExamPaperListQuery;
import com.learningplatform.exam.dto.ExamPaperSummaryResponse;
import com.learningplatform.exam.dto.ExamPaperWriteRequest;
import com.learningplatform.exam.dto.ReplacePaperQuestionsRequest;
import com.learningplatform.exam.service.ExamPaperService;
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
@RequestMapping("/api/publisher/papers")
public class PublisherExamPaperController {
    private final ExamPaperService paperService;

    public PublisherExamPaperController(ExamPaperService paperService) {
        this.paperService = paperService;
    }

    @GetMapping
    public ApiResponse<PageResult<ExamPaperSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute ExamPaperListQuery query
    ) {
        return ApiResponse.success(paperService.list(principal(authentication).userId(), query));
    }

    @PostMapping
    public ApiResponse<ExamPaperDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody ExamPaperWriteRequest request
    ) {
        return ApiResponse.success(paperService.create(principal(authentication).userId(), request));
    }

    @GetMapping("/{paperId}")
    public ApiResponse<ExamPaperDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long paperId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.detail(
                paperId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/{paperId}")
    public ApiResponse<ExamPaperDetailResponse> update(
            Authentication authentication,
            @PathVariable Long paperId,
            @Valid @RequestBody ExamPaperWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.update(
                paperId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @PutMapping("/{paperId}/questions")
    public ApiResponse<ExamPaperDetailResponse> replaceQuestions(
            Authentication authentication,
            @PathVariable Long paperId,
            @Valid @RequestBody ReplacePaperQuestionsRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(paperService.replaceQuestions(
                paperId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/{paperId}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long paperId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        paperService.delete(paperId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
