package com.learningplatform.learning.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.learning.dto.LearningProgressResponse;
import com.learningplatform.learning.dto.UpdateLearningProgressRequest;
import com.learningplatform.learning.service.LearningProgressService;
import com.learningplatform.learning.service.ContentInteractionService;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learning")
public class LearningProgressController {
    private final LearningProgressService progressService;
    private final ContentInteractionService interactionService;

    public LearningProgressController(
            LearningProgressService progressService,
            ContentInteractionService interactionService
    ) {
        this.progressService = progressService;
        this.interactionService = interactionService;
    }

    @PostMapping("/contents/{contentId}/start")
    public ApiResponse<LearningProgressResponse> start(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(progressService.start(
                principal.userId(),
                isAdmin(principal),
                contentId
        ));
    }

    @PutMapping("/contents/{contentId}/progress")
    public ApiResponse<LearningProgressResponse> update(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody UpdateLearningProgressRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(progressService.update(
                principal.userId(),
                isAdmin(principal),
                contentId,
                request
        ));
    }

    @GetMapping("/contents/{contentId}/progress")
    public ApiResponse<LearningProgressResponse> get(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        return ApiResponse.success(progressService.get(principal(authentication).userId(), contentId));
    }

    @GetMapping("/progress")
    public ApiResponse<List<LearningProgressResponse>> list(Authentication authentication) {
        return ApiResponse.success(progressService.list(principal(authentication).userId()));
    }

    @GetMapping("/favorites")
    public ApiResponse<List<ContentSummaryResponse>> favorites(Authentication authentication) {
        return ApiResponse.success(interactionService.favorites(principal(authentication).userId()));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
