package com.learningplatform.offline.web;

import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.offline.dto.OfflineTeachingDtos.RecommendationGenerateRequest;
import com.learningplatform.offline.dto.OfflineTeachingDtos.StudentPreferenceRequest;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationRequest;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherApplicationResponse;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherProfileResponse;
import com.learningplatform.offline.dto.OfflineTeachingDtos.TeacherRecommendationResponse;
import com.learningplatform.offline.dto.TeacherSearchQuery;
import com.learningplatform.offline.service.OfflineTeacherService;
import com.learningplatform.offline.service.TeacherRecommendationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/offline-teaching")
public class OfflineTeachingController {
    private final OfflineTeacherService teacherService;
    private final TeacherRecommendationService recommendationService;

    public OfflineTeachingController(
            OfflineTeacherService teacherService,
            TeacherRecommendationService recommendationService
    ) {
        this.teacherService = teacherService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/teachers")
    public ApiResponse<PageResult<TeacherProfileResponse>> teachers(
            @Valid @ModelAttribute TeacherSearchQuery query
    ) {
        return ApiResponse.success(teacherService.search(query));
    }

    @GetMapping("/teachers/{teacherId}")
    public ApiResponse<TeacherProfileResponse> teacher(
            @PathVariable Long teacherId
    ) {
        return ApiResponse.success(teacherService.publicProfile(teacherId));
    }

    @GetMapping("/teachers/by-user/{userId}")
    public ApiResponse<TeacherProfileResponse> teacherByUser(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(teacherService.publicProfileByUser(userId));
    }

    @GetMapping("/application")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<TeacherApplicationResponse> application(
            Authentication authentication
    ) {
        return ApiResponse.success(
                teacherService.currentApplication(userId(authentication))
                        .orElse(null)
        );
    }

    @PutMapping("/application")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<TeacherApplicationResponse> saveApplication(
            Authentication authentication,
            @Valid @RequestBody TeacherApplicationRequest request
    ) {
        return ApiResponse.success(
                teacherService.saveApplication(userId(authentication), request)
        );
    }

    @PostMapping("/application/submit")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    public ApiResponse<TeacherApplicationResponse> submit(
            Authentication authentication
    ) {
        return ApiResponse.success(
                teacherService.submit(userId(authentication))
        );
    }

    @GetMapping("/preference")
    public ApiResponse<StudentPreferenceRequest> preference(
            Authentication authentication
    ) {
        return ApiResponse.success(
                recommendationService.preference(userId(authentication))
        );
    }

    @PutMapping("/preference")
    public ApiResponse<StudentPreferenceRequest> savePreference(
            Authentication authentication,
            @Valid @RequestBody StudentPreferenceRequest request
    ) {
        return ApiResponse.success(recommendationService.savePreference(
                userId(authentication),
                request
        ));
    }

    @PostMapping("/recommendations")
    public ApiResponse<TeacherRecommendationResponse> recommend(
            Authentication authentication,
            @Valid @RequestBody RecommendationGenerateRequest request
    ) {
        return ApiResponse.success(recommendationService.generate(
                userId(authentication),
                request
        ));
    }

    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
