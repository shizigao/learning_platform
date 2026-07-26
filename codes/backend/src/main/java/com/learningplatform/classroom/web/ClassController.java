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
public class ClassController {
    private final ClassroomService classroomService;
    private final ClassAnnouncementService announcementService;
    private final LearningContentService contentService;
    private final ExamService examService;

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
    public ApiResponse<List<ClassSummaryResponse>> joined(Authentication authentication) {
        return ApiResponse.success(classroomService.joinedClasses(userId(authentication)));
    }

    @PostMapping("/join")
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
    public ApiResponse<ClassSummaryResponse> detail(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        return ApiResponse.success(classroomService.detail(classId, userId(authentication)));
    }

    @PostMapping("/{classId}/leave")
    public ApiResponse<Void> leave(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        classroomService.leave(classId, userId(authentication));
        return ApiResponse.success();
    }

    @GetMapping("/{classId}/members")
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
    public ApiResponse<List<AnnouncementResponse>> announcements(
            Authentication authentication,
            @PathVariable Long classId
    ) {
        return ApiResponse.success(announcementService.list(classId, userId(authentication)));
    }

    @GetMapping("/{classId}/contents")
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
    public ApiResponse<Void> deleteAnnouncement(
            Authentication authentication,
            @PathVariable Long classId,
            @PathVariable Long announcementId
    ) {
        announcementService.delete(classId, announcementId, userId(authentication));
        return ApiResponse.success();
    }

    private Long userId(Authentication authentication) {
        AuthenticatedUserPrincipal principal =
                AuthenticationPrincipalResolver.require(authentication);
        return principal.userId();
    }
}
