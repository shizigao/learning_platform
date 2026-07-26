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
public class UserProfileController {
    private final PublicUserProfileService profileService;
    private final UserAvatarService avatarService;
    private final AuthService authService;

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
    public ApiResponse<PageResult<PublicUserSummaryResponse>> search(
            @Valid @ModelAttribute UserSearchQuery query
    ) {
        return ApiResponse.success(profileService.search(query));
    }

    @GetMapping("/{userId}")
    public ApiResponse<PublicUserProfileResponse> profile(@PathVariable Long userId) {
        return ApiResponse.success(profileService.profile(userId));
    }

    @GetMapping("/{userId}/contents")
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            @PathVariable Long userId,
            @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.success(profileService.contents(userId, query));
    }

    @PostMapping(path = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ApiResponse<UserProfileResponse> deleteAvatar(Authentication authentication) {
        avatarService.delete(userId(authentication));
        return ApiResponse.success(authService.currentUser(authentication));
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<InputStreamResource> avatar(@PathVariable Long userId) {
        UserAvatar avatar = avatarService.getRequired(userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(avatar.getContentType()))
                .contentLength(avatar.getSizeBytes())
                .cacheControl(CacheControl.noCache())
                .body(new InputStreamResource(avatarService.open(avatar)));
    }

    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
