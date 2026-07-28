/* 文件职责：提供管理学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 提供管理学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AdminContentController {
    /** 委托分类执行对应领域规则。 */
    private final ContentCategoryService categoryService;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminContentController(
            ContentCategoryService categoryService,
            LearningContentService contentService
    ) {
        this.categoryService = categoryService;
        this.contentService = contentService;
    }

    @GetMapping("/categories")
    /** 处理 GET /categories 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<java.util.List<ContentCategoryResponse>> categories() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping("/categories")
    /** 处理 POST /categories 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentCategoryResponse> createCategory(
            @Valid @RequestBody CategoryWriteRequest request
    ) {
        return ApiResponse.success(categoryService.create(request));
    }

    @PutMapping("/categories/{categoryId}")
    /** 处理 PUT /categories/{categoryId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentCategoryResponse> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryWriteRequest request
    ) {
        return ApiResponse.success(categoryService.update(categoryId, request));
    }

    @DeleteMapping("/categories/{categoryId}")
    /** 处理 DELETE /categories/{categoryId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ApiResponse.success();
    }

    @GetMapping("/contents")
    /** 处理 GET /contents 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @Valid @ModelAttribute AdminContentListQuery query
    ) {
        return ApiResponse.success(contentService.listForAdmin(query));
    }

    @GetMapping("/contents/{contentId}")
    /** 处理 GET /contents/{contentId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> content(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = AuthenticationPrincipalResolver.require(authentication);
        return ApiResponse.success(contentService.publisherDetail(contentId, principal.userId(), true));
    }

    @PostMapping("/contents/{contentId}/approve")
    /** 处理 POST /contents/{contentId}/approve 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> approve(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.approve(contentId));
    }

    @PostMapping("/contents/{contentId}/reject")
    /** 处理 POST /contents/{contentId}/reject 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> reject(
            @PathVariable Long contentId,
            @Valid @RequestBody RejectContentRequest request
    ) {
        return ApiResponse.success(contentService.reject(contentId, request.reason()));
    }

    @PostMapping("/contents/{contentId}/offline")
    /** 处理 POST /contents/{contentId}/offline 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> takeOffline(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.takeOffline(contentId));
    }

    @PostMapping("/contents/{contentId}/publish")
    /** 处理 POST /contents/{contentId}/publish 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> republish(@PathVariable Long contentId) {
        return ApiResponse.success(contentService.republish(contentId));
    }
}
