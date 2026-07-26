package com.learningplatform.offline.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.offline.domain.TeacherProfileStatus;
import com.learningplatform.offline.dto.OfflineTeachingDtos.ReviewRequest;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationResponse;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationSummary;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherProfileResponse;
import com.learningplatform.offline.dto.TeacherApplicationAdminQuery;
import com.learningplatform.offline.service.OfflineTeacherService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin/offline-teachers")
public class AdminOfflineTeachingController {
    private final OfflineTeacherService teacherService;

    public AdminOfflineTeachingController(OfflineTeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/applications")
    public ApiResponse<PageResult<TeacherApplicationSummary>> applications(
            @Valid @ModelAttribute TeacherApplicationAdminQuery query
    ) {
        return ApiResponse.success(teacherService.adminApplications(query));
    }

    @GetMapping("/applications/{applicationId}")
    public ApiResponse<TeacherApplicationResponse> application(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(
                teacherService.adminApplication(applicationId)
        );
    }

    @GetMapping("/profiles/by-user/{userId}")
    public ApiResponse<TeacherProfileResponse> profileByUser(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(teacherService.adminProfileByUser(userId));
    }

    @PostMapping("/applications/{applicationId}/approve")
    public ApiResponse<TeacherApplicationResponse> approve(
            Authentication authentication,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(teacherService.approve(
                applicationId,
                userId(authentication)
        ));
    }

    @PostMapping("/applications/{applicationId}/reject")
    public ApiResponse<TeacherApplicationResponse> reject(
            Authentication authentication,
            @PathVariable Long applicationId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(teacherService.reject(
                applicationId,
                userId(authentication),
                request.reason()
        ));
    }

    @PutMapping("/profiles/{profileId}/suspend")
    public ApiResponse<TeacherProfileResponse> suspend(
            @PathVariable Long profileId,
            @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(teacherService.updateProfileStatus(
                profileId,
                TeacherProfileStatus.SUSPENDED,
                request.reason()
        ));
    }

    @PutMapping("/profiles/{profileId}/activate")
    public ApiResponse<TeacherProfileResponse> activate(
            @PathVariable Long profileId
    ) {
        return ApiResponse.success(teacherService.updateProfileStatus(
                profileId,
                TeacherProfileStatus.ACTIVE,
                null
        ));
    }

    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
