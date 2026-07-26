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
public class PublisherContentController {
    private final LearningContentService contentService;
    private final ContentFileService fileService;

    public PublisherContentController(
            LearningContentService contentService,
            ContentFileService fileService
    ) {
        this.contentService = contentService;
        this.fileService = fileService;
    }

    @GetMapping
    public ApiResponse<PageResult<ContentSummaryResponse>> list(
            Authentication authentication,
            @Valid @ModelAttribute PublisherContentListQuery query
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.listByPublisher(principal.userId(), query));
    }

    @GetMapping("/reference-candidates")
    public ApiResponse<PageResult<ContentSummaryResponse>> referenceCandidates(
            @Valid @ModelAttribute ContentReferenceSearchQuery query
    ) {
        return ApiResponse.success(contentService.listReferenceCandidates(query));
    }

    @PostMapping
    public ApiResponse<ContentDetailResponse> create(
            Authentication authentication,
            @Valid @RequestBody ContentWriteRequest request
    ) {
        return ApiResponse.success(contentService.create(principal(authentication).userId(), request));
    }

    @GetMapping("/{contentId}")
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
    public ApiResponse<ContentDetailResponse> update(
            Authentication authentication,
            @PathVariable Long contentId,
            @Valid @RequestBody ContentWriteRequest request
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.update(
                contentId,
                principal.userId(),
                isAdmin(principal),
                request
        ));
    }

    @DeleteMapping("/{contentId}")
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        contentService.delete(contentId, principal.userId(), isAdmin(principal));
        return ApiResponse.success();
    }

    @PostMapping("/{contentId}/submit")
    public ApiResponse<ContentDetailResponse> submit(
            Authentication authentication,
            @PathVariable Long contentId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(contentService.submit(contentId, principal.userId()));
    }

    @PostMapping("/{contentId}/files")
    public ApiResponse<ContentFileResponse> uploadFile(
            Authentication authentication,
            @PathVariable Long contentId,
            @RequestParam ContentFileRole fileRole,
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "文件排序值不能小于0") int sortOrder,
            @RequestParam(required = false) @Min(value = 0, message = "视频时长不能小于0") Integer durationSeconds
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
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
    public ApiResponse<FileUrlResponse> downloadUrl(
            Authentication authentication,
            @PathVariable Long contentId,
            @PathVariable Long fileId
    ) {
        AuthenticatedUserPrincipal principal = principal(authentication);
        return ApiResponse.success(new FileUrlResponse(fileService.downloadUrl(
                contentId,
                fileId,
                principal.userId(),
                isAdmin(principal)
        )));
    }

    private AuthenticatedUserPrincipal principal(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication);
    }

    private boolean isAdmin(AuthenticatedUserPrincipal principal) {
        return principal.roles().contains(RoleCode.ADMIN);
    }
}
