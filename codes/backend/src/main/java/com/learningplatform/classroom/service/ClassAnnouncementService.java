package com.learningplatform.classroom.service;

import com.learningplatform.classroom.domain.ClassAnnouncement;
import com.learningplatform.classroom.domain.ClassMember;
import com.learningplatform.classroom.domain.ClassRole;
import com.learningplatform.classroom.dto.AnnouncementResponse;
import com.learningplatform.classroom.dto.AnnouncementWriteRequest;
import com.learningplatform.classroom.mapper.ClassroomMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.service.UserAvatarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassAnnouncementService {
    private final ClassroomMapper mapper;
    private final ClassroomService classroomService;
    private final UserAvatarService avatarService;

    public ClassAnnouncementService(
            ClassroomMapper mapper,
            ClassroomService classroomService,
            UserAvatarService avatarService
    ) {
        this.mapper = mapper;
        this.classroomService = classroomService;
        this.avatarService = avatarService;
    }

    public List<AnnouncementResponse> list(Long classId, Long userId) {
        classroomService.requireActiveMember(classId, userId);
        return mapper.findAnnouncements(classId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public AnnouncementResponse create(
            Long classId,
            Long authorId,
            AnnouncementWriteRequest request
    ) {
        classroomService.requireManager(classId, authorId);
        ClassAnnouncement announcement = new ClassAnnouncement();
        announcement.setClassId(classId);
        announcement.setAuthorId(authorId);
        apply(announcement, request);
        if (mapper.insertAnnouncement(announcement) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "发布公告失败");
        }
        return response(required(announcement.getId()));
    }

    @Transactional
    public AnnouncementResponse update(
            Long classId,
            Long announcementId,
            Long requesterId,
            AnnouncementWriteRequest request
    ) {
        ClassMember requester = classroomService.requireManager(classId, requesterId);
        ClassAnnouncement announcement = requiredForClass(classId, announcementId);
        if (requester.getRole() != ClassRole.OWNER
                && !announcement.getAuthorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "班级管理员只能修改自己发布的公告");
        }
        apply(announcement, request);
        if (mapper.updateAnnouncement(announcement) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "公告状态已变化，请刷新后重试");
        }
        return response(required(announcementId));
    }

    @Transactional
    public void delete(Long classId, Long announcementId, Long requesterId) {
        ClassMember requester = classroomService.requireManager(classId, requesterId);
        ClassAnnouncement announcement = requiredForClass(classId, announcementId);
        if (requester.getRole() != ClassRole.OWNER
                && !announcement.getAuthorId().equals(requesterId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "班级管理员只能删除自己发布的公告");
        }
        if (mapper.softDeleteAnnouncement(announcementId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "公告状态已变化，请刷新后重试");
        }
    }

    private ClassAnnouncement required(Long announcementId) {
        return mapper.findAnnouncementById(announcementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在"));
    }

    private ClassAnnouncement requiredForClass(Long classId, Long announcementId) {
        ClassAnnouncement announcement = required(announcementId);
        if (!classId.equals(announcement.getClassId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return announcement;
    }

    private void apply(ClassAnnouncement announcement, AnnouncementWriteRequest request) {
        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());
        announcement.setPinned(Boolean.TRUE.equals(request.pinned()));
    }

    private AnnouncementResponse response(ClassAnnouncement announcement) {
        return AnnouncementResponse.from(
                announcement,
                avatarService.avatarUrl(announcement.getAuthorId())
        );
    }
}
