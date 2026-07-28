/* 文件职责：提供学习资料查询条件相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.dto.ContentCategoryResponse;
import com.learningplatform.content.dto.ContentCategorySearchQuery;
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
/**
 * 提供学习资料查询条件相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class ContentQueryController {
    /** 委托分类执行对应领域规则。 */
    private final ContentCategoryService categoryService;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;
    /** 委托访问权执行对应领域规则。 */
    private final ContentAccessService accessService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
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
    /** 处理 GET /categories 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ContentCategoryResponse>> categories() {
        return ApiResponse.success(categoryService.listEnabled());
    }

    @GetMapping("/categories/search")
    /** 处理 GET /categories/search 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentCategoryResponse>> searchCategories(
            @Valid @ModelAttribute ContentCategorySearchQuery query
    ) {
        return ApiResponse.success(categoryService.searchEnabled(query));
    }

    @GetMapping("/categories/{categoryId}")
    /** 处理 GET /categories/{categoryId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentCategoryResponse> category(@PathVariable Long categoryId) {
        return ApiResponse.success(ContentCategoryResponse.from(
                categoryService.getRequiredEnabled(categoryId)
        ));
    }

    @GetMapping("/contents")
    /** 处理 GET /contents 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @Valid @ModelAttribute ContentListQuery query
    ) {
        return ApiResponse.success(contentService.listPublished(query));
    }

    @GetMapping("/contents/{contentId}")
    /** 处理 GET /contents/{contentId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 GET /contents/{contentId}/files/{fileId}/preview-url 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 GET /contents/{contentId}/files/{fileId}/download-url 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
