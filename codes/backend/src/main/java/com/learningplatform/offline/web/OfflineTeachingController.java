/* 文件职责：提供线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 提供线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class OfflineTeachingController {
    /** 委托教师执行对应领域规则。 */
    private final OfflineTeacherService teacherService;
    /** 委托推荐执行对应领域规则。 */
    private final TeacherRecommendationService recommendationService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public OfflineTeachingController(
            OfflineTeacherService teacherService,
            TeacherRecommendationService recommendationService
    ) {
        this.teacherService = teacherService;
        this.recommendationService = recommendationService;
    }

    @GetMapping("/teachers")
    /** 处理 GET /teachers 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<TeacherProfileResponse>> teachers(
            @Valid @ModelAttribute TeacherSearchQuery query
    ) {
        return ApiResponse.success(teacherService.search(query));
    }

    @GetMapping("/teachers/{teacherId}")
    /** 处理 GET /teachers/{teacherId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherProfileResponse> teacher(
            @PathVariable Long teacherId
    ) {
        return ApiResponse.success(teacherService.publicProfile(teacherId));
    }

    @GetMapping("/teachers/by-user/{userId}")
    /** 处理 GET /teachers/by-user/{userId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherProfileResponse> teacherByUser(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(teacherService.publicProfileByUser(userId));
    }

    @GetMapping("/application")
    @PreAuthorize("hasAnyRole('PUBLISHER','ADMIN')")
    /** 处理 GET /application 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 PUT /application 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 POST /application/submit 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherApplicationResponse> submit(
            Authentication authentication
    ) {
        return ApiResponse.success(
                teacherService.submit(userId(authentication))
        );
    }

    @GetMapping("/preference")
    /** 处理 GET /preference 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<StudentPreferenceRequest> preference(
            Authentication authentication
    ) {
        return ApiResponse.success(
                recommendationService.preference(userId(authentication))
        );
    }

    @PutMapping("/preference")
    /** 处理 PUT /preference 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<StudentPreferenceRequest> savePreference(
            Authentication authentication,
            @Valid @RequestBody StudentPreferenceRequest request
    ) {
        return ApiResponse.success(recommendationService.savePreference(
                userId(authentication),
                request
        ));
    }

    // AI推荐教师
    @PostMapping("/recommendations")
    /** 处理 POST /recommendations 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherRecommendationResponse> recommend(
            Authentication authentication,
            @Valid @RequestBody RecommendationGenerateRequest request
    ) {
        // 点击generate
        return ApiResponse.success(recommendationService.generate(
                userId(authentication),
                request
        ));
    }

    /** 处理 POST /recommendations 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
