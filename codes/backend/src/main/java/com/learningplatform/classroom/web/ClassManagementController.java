/* 文件职责：提供班级Management相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.classroom.dto.ClassMemberRoleRequest;
import com.learningplatform.classroom.dto.ClassSummaryResponse;
import com.learningplatform.classroom.dto.ClassWriteRequest;
import com.learningplatform.classroom.dto.InviteEnabledRequest;
import com.learningplatform.classroom.dto.TransferOwnershipRequest;
import com.learningplatform.classroom.service.ClassroomService;
import com.learningplatform.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/class-management/classes")
/**
 * 提供班级Management相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class ClassManagementController {
    /** 委托班级执行对应领域规则。 */
    private final ClassroomService classroomService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ClassManagementController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ClassSummaryResponse>> managed(Authentication authentication) {
        return ApiResponse.success(classroomService.managedClasses(userId(authentication)));
    }

    @PostMapping
    /** 处理 POST 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> create(
            Authentication authentication,
            @Valid @RequestBody ClassWriteRequest request
    ) {
        return ApiResponse.success(classroomService.create(userId(authentication), request));
    }

    @PutMapping("/{classId}")
    /** 处理 PUT /{classId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> update(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @RequestBody ClassWriteRequest request
    ) {
        return ApiResponse.success(classroomService.update(
                classId,
                userId(authentication),
                request
        ));
    }

    @PostMapping("/{classId}/invite/regenerate")
    /** 处理 POST /{classId}/invite/regenerate 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> regenerateInvite(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        return ApiResponse.success(classroomService.regenerateInvite(
                classId,
                userId(authentication)
        ));
    }

    @PutMapping("/{classId}/invite")
    /** 处理 PUT /{classId}/invite 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> setInviteEnabled(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @RequestBody InviteEnabledRequest request
    ) {
        return ApiResponse.success(classroomService.setInviteEnabled(
                classId,
                userId(authentication),
                request.enabled()
        ));
    }

    @PutMapping("/{classId}/members/{targetUserId}/role")
    /** 处理 PUT /{classId}/members/{targetUserId}/role 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> updateMemberRole(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody ClassMemberRoleRequest request
    ) {
        classroomService.updateMemberRole(
                classId,
                userId(authentication),
                targetUserId,
                request.role()
        );
        return ApiResponse.success();
    }

    @DeleteMapping("/{classId}/members/{targetUserId}")
    /** 处理 DELETE /{classId}/members/{targetUserId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> removeMember(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long targetUserId
    ) {
        classroomService.removeMember(classId, userId(authentication), targetUserId);
        return ApiResponse.success();
    }

    @PostMapping("/{classId}/members/{targetUserId}/restore")
    /** 处理 POST /{classId}/members/{targetUserId}/restore 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> restoreMember(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long targetUserId
    ) {
        classroomService.restoreMember(classId, userId(authentication), targetUserId);
        return ApiResponse.success();
    }

    @PutMapping("/{classId}/owner")
    /** 处理 PUT /{classId}/owner 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> transferOwnership(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @RequestBody TransferOwnershipRequest request
    ) {
        return ApiResponse.success(classroomService.transferOwnership(
                classId,
                userId(authentication),
                request.userId()
        ));
    }

    @DeleteMapping("/{classId}")
    /** 处理 DELETE /{classId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> archive(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        classroomService.archive(classId, userId(authentication));
        return ApiResponse.success();
    }

    /** 处理 DELETE /{classId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
