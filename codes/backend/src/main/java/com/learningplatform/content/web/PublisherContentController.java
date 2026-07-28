/* 文件职责：提供发布者学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：学习资料、分类、文件、审核与访问控制；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.content.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.domain.ContentFileRole;
import com.learningplatform.content.dto.ContentDetailResponse;
import com.learningplatform.content.dto.ContentFileResponse;
import com.learningplatform.content.dto.ContentReferenceSearchQuery;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.dto.ContentWriteRequest;
import com.learningplatform.content.dto.FileUrlResponse;
import com.learningplatform.content.dto.PublisherContentListQuery;
import com.learningplatform.content.service.ContentFileService;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.user.domain.RoleCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/publisher/contents")
/**
 * 提供发布者学习资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class PublisherContentController {
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;
    /** 委托文件执行对应领域规则。 */
    private final ContentFileService fileService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public PublisherContentController(
            LearningContentService contentService,
            ContentFileService fileService
    ) {
        this.contentService = contentService;
        this.fileService = fileService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute PublisherContentListQuery query
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.listByPublisher(principal.userId(), query));
    }

    @GetMapping("/reference-candidates")
    /** 处理 GET /reference-candidates 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> referenceCandidates(
            @Valid @ModelAttribute ContentReferenceSearchQuery query
    ) {
        return ApiResponse.success(contentService.listReferenceCandidates(query));
    }

    @PostMapping
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody ContentWriteRequest request
    ) {
        return ApiResponse.success(contentService.create(principal(authentication).userId(), request));
    }

    @GetMapping("/{contentId}")
    /** 处理 GET /{contentId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> detail(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.publisherDetail(
                contentId,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @PutMapping("/{contentId}")
    /** 处理 PUT /{contentId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> update(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody ContentWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        // 点击update
        return ApiResponse.success(contentService.update(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/{contentId}")
    /** 处理 DELETE /{contentId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        contentService.delete(contentId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @PostMapping("/{contentId}/submit")
    /** 处理 POST /{contentId}/submit 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentDetailResponse> submit(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.submit(contentId, principal.userId()));
    }

    @PostMapping("/{contentId}/files")
    /** 处理 POST /{contentId}/files 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ContentFileResponse> uploadFile(
            Authentication authentication,
            @PathVariable Long contentId,
            @RequestParam ContentFileRole fileRole,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "文件排序值不能小于0") int sortOrder,
            @RequestParam(required = false) @Min(value = 0, message = "视频时长不能小于0") Integer durationSeconds
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        // 点击upload
        return ApiResponse.success(fileService.upload(
                contentId,
                fileRole,
                file,
                sortOrder,
                durationSeconds,
                principal.userId(),
                isAdmin(principal)
        ));
    }

    @DeleteMapping("/{contentId}/files/{fileId}")
    /** 处理 DELETE /{contentId}/files/{fileId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> deleteFile(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        fileService.delete(contentId, fileId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @GetMapping("/{contentId}/files/{fileId}/preview-url")
    /** 处理 GET /{contentId}/files/{fileId}/preview-url 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<FileUrlResponse> previewUrl(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(new FileUrlResponse(fileService.previewUrl(
                contentId,
                fileId,
                principal.userId(),
                isAdmin(principal)
        )));
    }

    @GetMapping("/{contentId}/files/{fileId}/download-url")
    /** 处理 GET /{contentId}/files/{fileId}/download-url 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<FileUrlResponse> downloadUrl(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        // 点击downloadUrl
        return ApiResponse.success(new FileUrlResponse(fileService.downloadUrl(
                contentId,
                fileId,
                principal.userId(),
                isAdmin(principal)
        )));
    }

    /** 执行 principal 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    /** 判断是否满足管理条件，不修改持久化状态。 */
    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
