/* 文件职责：提供管理线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：线下教师申请、审核、检索与推荐；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 提供管理线下教学教学相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class AdminOfflineTeachingController {
    /** 委托教师执行对应领域规则。 */
    private final OfflineTeacherService teacherService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminOfflineTeachingController(OfflineTeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/applications")
    /** 处理 GET /applications 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<TeacherApplicationSummary>> applications(
            @Valid @ModelAttribute TeacherApplicationAdminQuery query
    ) {
        return ApiResponse.success(teacherService.adminApplications(query));
    }

    @GetMapping("/applications/{applicationId}")
    /** 处理 GET /applications/{applicationId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherApplicationResponse> application(
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(
                teacherService.adminApplication(applicationId)
        );
    }

    @GetMapping("/profiles/by-user/{userId}")
    /** 处理 GET /profiles/by-user/{userId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherProfileResponse> profileByUser(
            @PathVariable Long userId
    ) {
        return ApiResponse.success(teacherService.adminProfileByUser(userId));
    }

    @PostMapping("/applications/{applicationId}/approve")
    /** 处理 POST /applications/{applicationId}/approve 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 POST /applications/{applicationId}/reject 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 PUT /profiles/{profileId}/suspend 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
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
    /** 处理 PUT /profiles/{profileId}/activate 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<TeacherProfileResponse> activate(
            @PathVariable Long profileId
    ) {
        return ApiResponse.success(teacherService.updateProfileStatus(
                profileId,
                TeacherProfileStatus.ACTIVE,
                null
        ));
    }

    /** 处理 PUT /profiles/{profileId}/activate 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        return AuthenticationPrincipalResolver.require(authentication).userId();
    }
}
