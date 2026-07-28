/* 文件职责：提供班级相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：HTTP 接口层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.classroom.web;

import com.learningplatform.auth.security.AuthenticatedUserPrincipal;
import com.learningplatform.auth.security.AuthenticationPrincipalResolver;
import com.learningplatform.classroom.dto.AnnouncementResponse;
import com.learningplatform.classroom.dto.AnnouncementWriteRequest;
import com.learningplatform.classroom.dto.ClassMemberListQuery;
import com.learningplatform.classroom.dto.ClassMemberResponse;
import com.learningplatform.classroom.dto.ClassSummaryResponse;
import com.learningplatform.classroom.dto.JoinClassRequest;
import com.learningplatform.classroom.service.ClassAnnouncementService;
import com.learningplatform.classroom.service.ClassroomService;
import com.learningplatform.common.api.ApiResponse;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.content.dto.ContentSummaryResponse;
import com.learningplatform.content.service.LearningContentService;
import com.learningplatform.exam.dto.ExamSummaryResponse;
import com.learningplatform.exam.service.ExamService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/classes")
/**
 * 提供班级相关 HTTP 接口，负责请求校验、身份解析、权限入口和统一响应封装。
 *
 * <p>职责边界：只处理 HTTP 协议和身份入口，不直接编写 SQL 或复制领域规则。</p>
 */
public class ClassController {
    /** 委托班级执行对应领域规则。 */
    private final ClassroomService classroomService;
    /** 委托公告执行对应领域规则。 */
    private final ClassAnnouncementService announcementService;
    /** 委托学习资料执行对应领域规则。 */
    private final LearningContentService contentService;
    /** 委托考试执行对应领域规则。 */
    private final ExamService examService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ClassController(
            ClassroomService classroomService,
            ClassAnnouncementService announcementService,
            LearningContentService contentService,
            ExamService examService
    ) {
        this.classroomService = classroomService;
        this.announcementService = announcementService;
        this.contentService = contentService;
        this.examService = examService;
    }

    @GetMapping
    /** 处理 GET 当前资源 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<ClassSummaryResponse>> joined(Authentication authentication) {
        return ApiResponse.success(classroomService.joinedClasses(userId(authentication)));
    }

    @PostMapping("/join")
    /** 处理 POST /join 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> join(
            Authentication authentication,
            @Valid @RequestBody JoinClassRequest request
    ) {
        return ApiResponse.success(classroomService.join(
                userId(authentication),
                request.inviteCode()
        ));
    }

    @GetMapping("/{classId}")
    /** 处理 GET /{classId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<ClassSummaryResponse> detail(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        return ApiResponse.success(classroomService.detail(classId, userId(authentication)));
    }

    @PostMapping("/{classId}/leave")
    /** 处理 POST /{classId}/leave 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> leave(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        classroomService.leave(classId, userId(authentication));
        return ApiResponse.success();
    }

    @GetMapping("/{classId}/members")
    /** 处理 GET /{classId}/members 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ClassMemberResponse>> members(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @ModelAttribute ClassMemberListQuery query
    ) {
        return ApiResponse.success(classroomService.members(
                classId,
                userId(authentication),
                query
        ));
    }

    @GetMapping("/{classId}/announcements")
    /** 处理 GET /{classId}/announcements 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<List<AnnouncementResponse>> announcements(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        return ApiResponse.success(announcementService.list(classId, userId(authentication)));
    }

    @GetMapping("/{classId}/contents")
    /** 处理 GET /{classId}/contents 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ContentSummaryResponse>> contents(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.success(contentService.listForClass(
                classId,
                userId(authentication),
                query
        ));
    }

    @GetMapping("/{classId}/exams")
    /** 处理 GET /{classId}/exams 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<PageResult<ExamSummaryResponse>> exams(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @ModelAttribute PageQuery query
    ) {
        return ApiResponse.success(examService.listForClass(
                classId,
                userId(authentication),
                query
        ));
    }

    @PostMapping("/{classId}/announcements")
    /** 处理 POST /{classId}/announcements 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AnnouncementResponse> createAnnouncement(
            Authentication authentication,
            @PathVariable Long classId,
            @Valid @RequestBody AnnouncementWriteRequest request
    ) {
        return ApiResponse.success(announcementService.create(
                classId,
                userId(authentication),
                request
        ));
    }

    @PutMapping("/{classId}/announcements/{announcementId}")
    /** 处理 PUT /{classId}/announcements/{announcementId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<AnnouncementResponse> updateAnnouncement(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long announcementId,
            @Valid @RequestBody AnnouncementWriteRequest request
    ) {
        return ApiResponse.success(announcementService.update(
                classId,
                announcementId,
                userId(authentication),
                request
        ));
    }

    @DeleteMapping("/{classId}/announcements/{announcementId}")
    /** 处理 DELETE /{classId}/announcements/{announcementId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    public ApiResponse<Void> deleteAnnouncement(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long announcementId
    ) {
        announcementService.delete(classId, announcementId, userId(authentication));
        return ApiResponse.success();
    }

    /** 处理 DELETE /{classId}/announcements/{announcementId} 请求，完成参数接收、当前用户解析并返回统一 API 响应。 */
    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
