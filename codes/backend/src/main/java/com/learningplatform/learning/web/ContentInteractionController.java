package com.learningplatform.learning.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.learning.dto.ContentCommentResponse;
import com.learningplatform.learning.dto.ContentReactionResponse;
import com.learningplatform.learning.dto.CreateCommentRequest;
import com.learningplatform.learning.service.ContentInteractionService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contents/{contentId}")
public class ContentInteractionController {
    private final ContentInteractionService interactionService;

    public ContentInteractionController(ContentInteractionService interactionService) {
        this.interactionService = interactionService;
    }

    @GetMapping("/reactions")
    public ApiResponse<ContentReactionResponse> state(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.state(contentId, principal.userId()));
    }

    @PostMapping("/like")
    public ApiResponse<ContentReactionResponse> like(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.like(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/like")
    public ApiResponse<ContentReactionResponse> unlike(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        return ApiResponse.success(interactionService.unlike(
                contentId,
                principal(authentication).userId()
        ));
    }

    @PostMapping("/favorite")
    public ApiResponse<ContentReactionResponse> favorite(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        return ApiResponse.success(interactionService.favorite(
                contentId,
                principal(authentication).userId()
        ));
    }

    @DeleteMapping("/favorite")
    public ApiResponse<ContentReactionResponse> unfavorite(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        return ApiResponse.success(interactionService.unfavorite(
                contentId,
                principal(authentication).userId()
        ));
    }

    @PostMapping("/comments")
    public ApiResponse<ContentCommentResponse> comment(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(interactionService.comment(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @GetMapping("/comments")
    public ApiResponse<PageResult<ContentCommentResponse>> comments(
            @PathVariable Long contentId,
            @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.success(interactionService.comments(contentId, query));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
