package com.learningplatform.content.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.dto.AdminContentListQuery;
import com.learningplatform.content.dto.CategoryWriteRequest;
import com.learningplatform.content.dto.ContentCategoryResponse;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.RejectContentRequest;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.service.ContentCategoryService;
import com.learningplatform.content.service.LearningContentService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminContentController {
    private final ContentCategoryService categoryService;
    private final LearningContentService contentService;

    public AdminContentController(
            ContentCategoryService categoryService,
            LearningContentService contentService
    ) {
        this.categoryService = categoryService;
        this.contentService = contentService;
    }

    @GetMapping("/categories")
    public ApiResponse<java.util.List<ContentCategoryResponse>> categories() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping("/categories")
    public ApiResponse<ContentCategoryResponse> createCategory(
            @Valid @RequestBody CategoryWriteRequest request
    ) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<ContentCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryWriteRequest request
    ) {
        return ApiResponse.success(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success();
    }

    @GetMapping("/contents")
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @Valid @ModelAttribute AdminContentListQuery query
    ) {
        return ApiResponse.success(contentService.listForAdmin(query));
    }

    @GetMapping("/contents/{contentId}")
    public ApiResponse<ContentDetailResponse> content(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = AuthenticationPrincipalResolver.require(authentication);
        return ApiResponse.success(contentService.publisherDetail(contentId, principal.userId(), true));
    }

    @PostMapping("/contents/{contentId}/approve")
    public ApiResponse<ContentDetailResponse> approve(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.approve(contentId));
    }

    @PostMapping("/contents/{contentId}/reject")
    public ApiResponse<ContentDetailResponse> reject(
            @PathVariable Long contentId,
            @Valid @RequestBody RejectContentRequest request
    ) {
        return ApiResponse.success(contentService.reject(contentId, request.reason()));
    }

    @PostMapping("/contents/{contentId}/offline")
    public ApiResponse<ContentDetailResponse> takeOffline(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.takeOffline(contentId));
    }

    @PostMapping("/contents/{contentId}/publish")
    public ApiResponse<ContentDetailResponse> republish(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.republish(contentId));
    }
}
