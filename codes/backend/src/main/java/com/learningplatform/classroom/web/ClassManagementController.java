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
public class ClassManagementController {
    private final ClassroomService classroomService;

    public ClassManagementController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    public ApiResponse<List<ClassSummaryResponse>> managed(Authentication authentication) {
        return ApiResponse.success(classroomService.managedClasses(userId(authentication)));
    }

    @PostMapping
    public ApiResponse<ClassSummaryResponse> create(
            Authentication authentication,
            @Valid @RequestBody ClassWriteRequest request
    ) {
        return ApiResponse.success(classroomService.create(userId(authentication), request));
    }

    @PutMapping("/{classId}")
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
    public ApiResponse<Void> removeMember(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long targetUserId
    ) {
        classroomService.removeMember(classId, userId(authentication), targetUserId);
        return ApiResponse.success();
    }

    @PostMapping("/{classId}/members/{targetUserId}/restore")
    public ApiResponse<Void> restoreMember(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long targetUserId
    ) {
        classroomService.restoreMember(classId, userId(authentication), targetUserId);
        return ApiResponse.success();
    }

    @PutMapping("/{classId}/owner")
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
    public ApiResponse<Void> archive(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        classroomService.archive(classId, userId(authentication));
        return ApiResponse.success();
    }

    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
