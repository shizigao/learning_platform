package com.learningplatform.content.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.dto.ContentCategoryResponse;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.ContentListQuery;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.dto.FileUrlResponse;
import com.learningplatform.content.service.ContentAccessService;
import com.learningplatform.content.service.ContentCategoryService;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ContentQueryController {
    private final ContentCategoryService categoryService;
    private final LearningContentService contentService;
    private final ContentAccessService accessService;

    public ContentQueryController(
            ContentCategoryService categoryService,
            LearningContentService contentService,
            ContentAccessService accessService
    ) {
        this.categoryService = categoryService;
        this.contentService = contentService;
        this.accessService = accessService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<ContentCategoryResponse>> categories() {
        return ApiResponse.success(categoryService.listEnabled());
    }

    @GetMapping("/contents")
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @Valid @ModelAttribute ContentListQuery query
    ) {
        return ApiResponse.success(contentService.listPublished(query));
    }

    @GetMapping("/contents/{contentId}")
    public ApiResponse<ContentDetailResponse> content(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = AuthenticationPrincipalResolver.require(authentication);
        return ApiResponse.success(contentService.publishedDetail(
                contentId,
                principal.userId(),
                principal.roles().contains(RoleCode.ADMIN)
        ));
    }

    @GetMapping("/contents/{contentId}/files/{fileId}/preview-url")
    public ApiResponse<FileUrlResponse> previewUrl(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = AuthenticationPrincipalResolver.require(authentication);
        return ApiResponse.success(new FileUrlResponse(accessService.previewUrl(
                contentId,
                fileId,
                principal.userId(),
                principal.roles().contains(RoleCode.ADMIN)
        )));
    }

    @GetMapping("/contents/{contentId}/files/{fileId}/download-url")
    public ApiResponse<FileUrlResponse> downloadUrl(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = AuthenticationPrincipalResolver.require(authentication);
        return ApiResponse.success(new FileUrlResponse(accessService.downloadUrl(
                contentId,
                fileId,
                principal.userId(),
                principal.roles().contains(RoleCode.ADMIN)
        )));
    }
}
