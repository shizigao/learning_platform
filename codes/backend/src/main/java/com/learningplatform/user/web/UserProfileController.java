/* 文件职责：提供用户资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.web;

import com.learningplatform.auth.dto.UserProfileResponse;
import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.auth.service.AuthService;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.user.domain.UserAvatar;
import com.learningplatform.user.dto.AvatarUploadResponse;
import com.learningplatform.user.dto.PublicUserProfileResponse;
import com.learningplatform.user.dto.PublicUserSummaryResponse;
import com.learningplatform.user.dto.UserSearchQuery;
import com.learningplatform.user.service.PublicUserProfileService;
import com.learningplatform.user.service.UserAvatarService;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/users")
/**
 * 提供用户资料相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class UserProfileController {
    /** 委托资料执行对应领域规则。 */
    private final PublicUserProfileService profileService;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;
    /** 委托认证执行对应领域规则。 */
    private final AuthService authService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public UserProfileController(
            PublicUserProfileService profileService,
            UserAvatarService avatarService,
            AuthService authService
    ) {
        this.profileService = profileService;
        this.avatarService = avatarService;
        this.authService = authService;
    }

    @GetMapping("/search")
    /** 处理 GET /search 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<PublicUserSummaryResponse>> search(
            @Valid @ModelAttribute UserSearchQuery query
    ) {
        return ApiResponse.success(profileService.search(query));
    }

    @GetMapping("/{userId}")
    /** 处理 GET /{userId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PublicUserProfileResponse> profile(@PathVariable Long userId) {
        return ApiResponse.success(profileService.profile(userId));
    }

    @GetMapping("/{userId}/contents")
    /** 处理 GET /{userId}/contents 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @PathVariable Long userId,
            @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.success(profileService.contents(userId, query));
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AvatarUploadResponse> uploadAvatar(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        Long userId = userId(authentication);
        return ApiResponse.success(new AvatarUploadResponse(
                avatarService.upload(userId, file)
        ));
    }

    @DeleteMapping("/me/avatar")
    /** 处理 DELETE /me/avatar 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<UserProfileResponse> deleteAvatar(Authentication authentication) {
        avatarService.delete(userId(authentication));
        return ApiResponse.success(authService.currentUser(authentication));
    }

    @GetMapping("/{userId}/avatar")
    /** 处理 GET /{userId}/avatar 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ResponseEntity<InputStreamResource> avatar(@PathVariable Long userId) {
        UserAvatar avatar = avatarService.getRequired(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .contentLength(avatar.getSizeBytes())
                .cacheControl(CacheControl.noCache())
                .body(new InputStreamResource(avatarService.open(avatar)));
    }

    /** 处理 GET /{userId}/avatar 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
