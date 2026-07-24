package com.learningplatform.ai.web;

import com.learningplatform.ai.dto.AiConversationCreateRequest;
import com.learningplatform.ai.dto.AiConversationResponse;
import com.learningplatform.ai.dto.AiExplanationRequest;
import com.learningplatform.ai.dto.AiExplanationResponse;
import com.learningplatform.ai.dto.AiSummaryGenerateRequest;
import com.learningplatform.ai.dto.AiSummaryResponse;
import com.learningplatform.ai.dto.AiTaskResponse;
import com.learningplatform.ai.dto.AiUsageRecordResponse;
import com.learningplatform.ai.service.AiConversationService;
import com.learningplatform.ai.service.AiQuotaService;
import com.learningplatform.ai.service.AiSummaryService;
import com.learningplatform.ai.service.AiTaskLifecycleService;
import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiLearningController {
    private final AiSummaryService summaryService;
    private final AiConversationService conversationService;
    private final AiTaskLifecycleService taskService;
    private final AiQuotaService quotaService;

    public AiLearningController(
            AiSummaryService summaryService,
            AiConversationService conversationService,
            AiTaskLifecycleService taskService,
            AiQuotaService quotaService
    ) {
        this.summaryService = summaryService;
        this.conversationService = conversationService;
        this.taskService = taskService;
        this.quotaService = quotaService;
    }

    @PostMapping("/contents/{contentId}/summaries")
    public ApiResponse<AiSummaryResponse> generateSummary(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody(required = false) AiSummaryGenerateRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(summaryService.generate(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request == null ? null : request.requestId()
        ));
    }

    @GetMapping("/contents/{contentId}/summaries/latest")
    public ApiResponse<AiSummaryResponse> latestSummary(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(summaryService.latest(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<AiTaskResponse> task(
            Authentication authentication,
            @PathVariable Long taskId
    ) {
        return ApiResponse.success(taskService.detail(
                taskId,
                principal(authentication).userId()
        ));
    }

    @GetMapping("/tasks")
    public ApiResponse<List<AiTaskResponse>> tasks(
            Authentication authentication
    ) {
        return ApiResponse.success(taskService.list(
                principal(authentication).userId()
        ));
    }

    @GetMapping("/usage-records")
    public ApiResponse<List<AiUsageRecordResponse>> usageRecords(
            Authentication authentication
    ) {
        return ApiResponse.success(quotaService.records(
                principal(authentication).userId()
        ));
    }

    @PostMapping("/contents/{contentId}/conversations")
    public ApiResponse<AiConversationResponse> createConversation(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody(required = false) AiConversationCreateRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.create(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request == null ? null : request.title()
        ));
    }

    @GetMapping("/contents/{contentId}/conversations")
    public ApiResponse<List<AiConversationResponse>> conversations(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.list(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<AiConversationResponse> conversation(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.detail(
                conversationId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ApiResponse<AiExplanationResponse> explain(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody AiExplanationRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(conversationService.explain(
                conversationId,
                principal.userId(),
                isAdmin(principal),
                request.requestId(),
                request.question()
        ));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
