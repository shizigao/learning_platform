package com.learningplatform.classroom.service;

import com.learningplatform.classroom.domain.ClassMember;
import com.learningplatform.classroom.domain.ClassMemberStatus;
import com.learningplatform.classroom.domain.ClassRole;
import com.learningplatform.classroom.domain.ClassStatus;
import com.learningplatform.classroom.domain.LearningClass;
import com.learningplatform.classroom.dto.ClassMemberListQuery;
import com.learningplatform.classroom.dto.ClassMemberResponse;
import com.learningplatform.classroom.dto.ClassSummaryResponse;
import com.learningplatform.classroom.dto.ClassWriteRequest;
import com.learningplatform.classroom.mapper.ClassroomMapper;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import com.learningplatform.user.service.UserAvatarService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class ClassroomService {
    private static final char[] INVITE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int INVITE_LENGTH = 12;

    private final ClassroomMapper mapper;
    private final UserService userService;
    private final RoleService roleService;
    private final UserAvatarService avatarService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ClassroomService(
            ClassroomMapper mapper,
            UserService userService,
            RoleService roleService,
            UserAvatarService avatarService
    ) {
        this.mapper = mapper;
        this.userService = userService;
        this.roleService = roleService;
        this.avatarService = avatarService;
    }

    public List<ClassSummaryResponse> joinedClasses(Long userId) {
        return mapper.findJoinedClasses(userId).stream()
                .map(classroom -> response(classroom, userId))
                .toList();
    }

    public List<ClassSummaryResponse> managedClasses(Long userId) {
        return mapper.findManagedClasses(userId).stream()
                .map(classroom -> response(classroom, userId))
                .toList();
    }

    public ClassSummaryResponse detail(Long classId, Long userId) {
        LearningClass classroom = getRequired(classId);
        requireActiveMember(classId, userId);
        return response(classroom, userId);
    }

    @Transactional
    public ClassSummaryResponse create(Long ownerId, ClassWriteRequest request) {
        User owner = userService.getRequiredById(ownerId);
        if (owner.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号不可创建班级");
        }
        LearningClass classroom = new LearningClass();
        classroom.setOwnerId(ownerId);
        classroom.setName(request.name().trim());
        classroom.setDescription(normalize(request.description()));
        classroom.setInviteCode(generateUniqueInviteCode());
        classroom.setInviteEnabled(true);
        classroom.setStatus(ClassStatus.ACTIVE);
        classroom.setVersion(0);
        if (mapper.insertClass(classroom) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建班级失败");
        }

        ClassMember ownerMember = new ClassMember();
        ownerMember.setClassId(classroom.getId());
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(ClassRole.OWNER);
        ownerMember.setStatus(ClassMemberStatus.ACTIVE);
        if (mapper.insertMember(ownerMember) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建班级拥有者失败");
        }
        return response(getRequired(classroom.getId()), ownerId);
    }

    @Transactional
    public ClassSummaryResponse update(
            Long classId,
            Long requesterId,
            ClassWriteRequest request
    ) {
        requireOwner(classId, requesterId);
        LearningClass classroom = getRequired(classId);
        classroom.setName(request.name().trim());
        classroom.setDescription(normalize(request.description()));
        if (mapper.updateClass(classroom) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "班级状态已变化，请刷新后重试");
        }
        return response(getRequired(classId), requesterId);
    }

    @Transactional
    public ClassSummaryResponse join(Long userId, String inviteCode) {
        String normalized = normalizeInviteCode(inviteCode);
        LearningClass classroom = mapper.findClassByInviteCode(normalized)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "班级邀请码无效"));
        if (classroom.getStatus() != ClassStatus.ACTIVE || !Boolean.TRUE.equals(classroom.getInviteEnabled())) {
            throw new BusinessException(ErrorCode.CONFLICT, "该班级当前不允许通过邀请码加入");
        }
        ClassMember existing = mapper.findMember(classroom.getId(), userId).orElse(null);
        if (existing != null) {
            if (existing.getStatus() == ClassMemberStatus.ACTIVE) {
                return response(classroom, userId);
            }
            if (existing.getStatus() == ClassMemberStatus.REMOVED) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "你已被移出该班级，请联系班级拥有者");
            }
            if (mapper.rejoinMember(classroom.getId(), userId) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "重新加入班级失败，请重试");
            }
        } else {
            ClassMember member = new ClassMember();
            member.setClassId(classroom.getId());
            member.setUserId(userId);
            member.setRole(ClassRole.MEMBER);
            member.setStatus(ClassMemberStatus.ACTIVE);
            if (mapper.insertMember(member) != 1) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "加入班级失败");
            }
        }
        return response(classroom, userId);
    }

    @Transactional
    public void leave(Long classId, Long userId) {
        ClassMember member = requireActiveMember(classId, userId);
        if (member.getRole() == ClassRole.OWNER) {
            throw new BusinessException(ErrorCode.CONFLICT, "班级拥有者必须先转让班级后才能退出");
        }
        if (mapper.updateMemberStatus(classId, userId, ClassMemberStatus.LEFT) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "退出班级失败，请重试");
        }
    }

    public PageResult<ClassMemberResponse> members(
            Long classId,
            Long requesterId,
            ClassMemberListQuery query
    ) {
        requireActiveMember(classId, requesterId);
        String keyword = normalize(query.getKeyword());
        long total = mapper.countActiveMembersByKeyword(classId, keyword);
        List<ClassMemberResponse> items = mapper.findActiveMembers(
                        classId,
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(member -> ClassMemberResponse.from(
                        member,
                        avatarService.avatarUrl(member.getUserId())
                ))
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    @Transactional
    public ClassSummaryResponse regenerateInvite(Long classId, Long requesterId) {
        requireOwner(classId, requesterId);
        String inviteCode = generateUniqueInviteCode();
        if (mapper.updateInviteCode(classId, inviteCode) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "更新邀请码失败，请重试");
        }
        return response(getRequired(classId), requesterId);
    }

    @Transactional
    public ClassSummaryResponse setInviteEnabled(
            Long classId,
            Long requesterId,
            boolean enabled
    ) {
        requireOwner(classId, requesterId);
        if (mapper.updateInviteEnabled(classId, enabled) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "更新邀请码状态失败，请重试");
        }
        return response(getRequired(classId), requesterId);
    }

    @Transactional
    public void updateMemberRole(
            Long classId,
            Long requesterId,
            Long targetUserId,
            ClassRole role
    ) {
        requireOwner(classId, requesterId);
        if (role != ClassRole.ADMIN && role != ClassRole.MEMBER) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只能设置为班级管理员或普通成员");
        }
        ClassMember target = requireActiveMember(classId, targetUserId);
        if (target.getRole() == ClassRole.OWNER) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能修改班级拥有者角色");
        }
        if (role == ClassRole.ADMIN && !hasPublisherOrAdminRole(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有发布者或系统管理员可以成为班级管理员");
        }
        if (mapper.updateMemberRole(classId, targetUserId, role) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "成员角色更新失败，请重试");
        }
    }

    @Transactional
    public void removeMember(Long classId, Long requesterId, Long targetUserId) {
        ClassMember requester = requireManager(classId, requesterId);
        ClassMember target = requireActiveMember(classId, targetUserId);
        if (target.getRole() == ClassRole.OWNER) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能移除班级拥有者");
        }
        if (requester.getRole() == ClassRole.ADMIN && target.getRole() != ClassRole.MEMBER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "班级管理员只能移除普通成员");
        }
        if (mapper.updateMemberStatus(classId, targetUserId, ClassMemberStatus.REMOVED) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "移除成员失败，请重试");
        }
    }

    @Transactional
    public void restoreMember(Long classId, Long requesterId, Long targetUserId) {
        requireOwner(classId, requesterId);
        if (mapper.restoreMember(classId, targetUserId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "该用户当前不处于被移除状态");
        }
    }

    @Transactional
    public ClassSummaryResponse transferOwnership(
            Long classId,
            Long requesterId,
            Long targetUserId
    ) {
        requireOwner(classId, requesterId);
        if (requesterId.equals(targetUserId)) {
            return response(getRequired(classId), requesterId);
        }
        requireActiveMember(classId, targetUserId);
        if (!hasPublisherOrAdminRole(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新拥有者必须是发布者或系统管理员");
        }
        if (mapper.updateMemberRole(classId, requesterId, ClassRole.MEMBER) != 1
                || mapper.updateMemberRole(classId, targetUserId, ClassRole.OWNER) != 1
                || mapper.updateOwner(classId, targetUserId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "转让班级失败，请重试");
        }
        return response(getRequired(classId), requesterId);
    }

    @Transactional
    public void archive(Long classId, Long requesterId) {
        requireOwner(classId, requesterId);
        if (mapper.countActiveClassExams(classId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "班级仍有未结束考试，请先取消或等待考试结束");
        }
        if (mapper.archiveClass(classId) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "班级状态已变化，请刷新后重试");
        }
    }

    public LearningClass getRequired(Long classId) {
        LearningClass classroom = mapper.findClassById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "班级不存在"));
        if (classroom.getStatus() != ClassStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "班级不存在或已解散");
        }
        return classroom;
    }

    public ClassMember requireActiveMember(Long classId, Long userId) {
        getRequired(classId);
        ClassMember member = mapper.findMember(classId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "你不是该班级成员"));
        if (member.getStatus() != ClassMemberStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "你不是该班级的有效成员");
        }
        return member;
    }

    public ClassMember requireManager(Long classId, Long userId) {
        ClassMember member = requireActiveMember(classId, userId);
        if (member.getRole() != ClassRole.OWNER && member.getRole() != ClassRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅班级拥有者或管理员可执行此操作");
        }
        return member;
    }

    public ClassMember requireOwner(Long classId, Long userId) {
        ClassMember member = requireActiveMember(classId, userId);
        if (member.getRole() != ClassRole.OWNER) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅班级拥有者可执行此操作");
        }
        return member;
    }

    public boolean isActiveMember(Long classId, Long userId) {
        if (userId == null) return false;
        return mapper.findMember(classId, userId)
                .filter(member -> member.getStatus() == ClassMemberStatus.ACTIVE)
                .isPresent();
    }

    public void requireManageableClasses(List<Long> classIds, Long userId) {
        if (classIds == null || classIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "班级发放至少需要选择一个班级");
        }
        if (classIds.size() > 50 || new HashSet<>(classIds).size() != classIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "班级选择重复或数量超过限制");
        }
        classIds.forEach(classId -> requireManager(classId, userId));
    }

    private ClassSummaryResponse response(LearningClass classroom, Long userId) {
        ClassMember member = mapper.findMember(classroom.getId(), userId).orElse(null);
        ClassRole role = member != null && member.getStatus() == ClassMemberStatus.ACTIVE
                ? member.getRole()
                : null;
        boolean owner = role == ClassRole.OWNER;
        return new ClassSummaryResponse(
                classroom.getId(),
                classroom.getOwnerId(),
                classroom.getName(),
                classroom.getDescription(),
                classroom.getStatus(),
                role,
                mapper.countActiveMembers(classroom.getId()),
                owner ? classroom.getInviteCode() : null,
                owner ? classroom.getInviteEnabled() : null,
                classroom.getCreatedAt(),
                classroom.getUpdatedAt()
        );
    }

    private boolean hasPublisherOrAdminRole(Long userId) {
        Set<RoleCode> roles = roleService.findRoleCodesByUserId(userId);
        return roles.contains(RoleCode.PUBLISHER) || roles.contains(RoleCode.ADMIN);
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder code = new StringBuilder(INVITE_LENGTH);
            for (int index = 0; index < INVITE_LENGTH; index++) {
                code.append(INVITE_ALPHABET[secureRandom.nextInt(INVITE_ALPHABET.length)]);
            }
            if (!mapper.inviteCodeExists(code.toString())) return code.toString();
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成班级邀请码失败");
    }

    private String normalizeInviteCode(String inviteCode) {
        return inviteCode == null ? "" : inviteCode.trim().toUpperCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
