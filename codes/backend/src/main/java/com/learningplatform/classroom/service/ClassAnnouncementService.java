/* 文件职责：实现班级公告业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：班级、成员、公告与班级资源范围；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现班级公告业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ClassAnnouncementService {
    /** 保存mapper，供该类型的业务逻辑读取或更新。 */
    private final ClassroomMapper mapper;
    /** 委托班级执行对应领域规则。 */
    private final ClassroomService classroomService;
    /** 委托头像执行对应领域规则。 */
    private final UserAvatarService avatarService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ClassAnnouncementService(
            ClassroomMapper mapper,
            ClassroomService classroomService,
            UserAvatarService avatarService
    ) {
        this.mapper = mapper;
        this.classroomService = classroomService;
        this.avatarService = avatarService;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public List<AnnouncementResponse> list(Long classId, Long userId) {
        classroomService.requireActiveMember(classId, userId);
        return mapper.findAnnouncements(classId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
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
    /** 更新，通过返回值或版本条件识别并发状态变化。 */
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
    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
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

    /** 校验d及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ClassAnnouncement required(Long announcementId) {
        return mapper.findAnnouncementById(announcementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "公告不存在"));
    }

    /** 校验dFor班级及相关业务前置条件，不满足时抛出明确业务异常。 */
    private ClassAnnouncement requiredForClass(Long classId, Long announcementId) {
        ClassAnnouncement announcement = required(announcementId);
        if (!classId.equals(announcement.getClassId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        return announcement;
    }

    /** 执行 apply 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void apply(ClassAnnouncement announcement, AnnouncementWriteRequest request) {
        announcement.setTitle(request.title().trim());
        announcement.setBody(request.body().trim());
        announcement.setPinned(Boolean.TRUE.equals(request.pinned()));
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private AnnouncementResponse response(ClassAnnouncement announcement) {
        return AnnouncementResponse.from(
                announcement,
                avatarService.avatarUrl(announcement.getAuthorId())
        );
    }
}
