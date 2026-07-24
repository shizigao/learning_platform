package com.learningplatform.admin.service;

import com.learningplatform.admin.dto.AdminUserListQuery;
import com.learningplatform.admin.dto.AdminUserResponse;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.page.PageResult;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.mapper.UserMapper;
import com.learningplatform.user.service.RoleService;
import com.learningplatform.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {
    private final UserMapper userMapper;
    private final UserService userService;
    private final RoleService roleService;

    public AdminUserService(
            UserMapper userMapper,
            UserService userService,
            RoleService roleService
    ) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.roleService = roleService;
    }

    public PageResult<AdminUserResponse> list(AdminUserListQuery query) {
        String keyword = normalize(query.getKeyword());
        long total = userMapper.countForAdmin(
                query.getStatus(),
                query.getRole(),
                keyword
        );
        List<AdminUserResponse> items = userMapper.findForAdmin(
                        query.getStatus(),
                        query.getRole(),
                        keyword,
                        query.offset(),
                        query.getPageSize()
                ).stream()
                .map(this::response)
                .toList();
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    public AdminUserResponse detail(Long userId) {
        return response(userService.getRequiredById(userId));
    }

    @Transactional
    public AdminUserResponse updateStatus(
            Long operatorId,
            Long userId,
            UserStatus status
    ) {
        if (status != UserStatus.ACTIVE && status != UserStatus.DISABLED) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "管理员只能启用或禁用账号"
            );
        }
        lockUser(userId);
        User user = userService.getRequiredById(userId);
        if (operatorId.equals(userId) && status != UserStatus.ACTIVE) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "不能禁用当前登录的管理员账号"
            );
        }
        if (user.getStatus() != status) {
            userService.updateStatus(userId, status);
        }
        return detail(userId);
    }

    @Transactional
    public AdminUserResponse replaceRoles(
            Long operatorId,
            Long userId,
            Set<RoleCode> requestedRoles
    ) {
        lockUser(userId);
        userService.getRequiredById(userId);
        Set<RoleCode> targetRoles = Set.copyOf(requestedRoles);
        if (operatorId.equals(userId) && !targetRoles.contains(RoleCode.ADMIN)) {
            throw new BusinessException(
                    ErrorCode.CONFLICT,
                    "不能移除当前登录账号的管理员角色"
            );
        }

        Set<RoleCode> currentRoles = roleService.findRoleCodesByUserId(userId);
        for (RoleCode role : currentRoles) {
            if (!targetRoles.contains(role)) {
                roleService.removeRole(userId, role);
            }
        }
        for (RoleCode role : targetRoles) {
            if (!currentRoles.contains(role)) {
                roleService.assignRole(userId, role, operatorId);
            }
        }
        return detail(userId);
    }

    private void lockUser(Long userId) {
        userMapper.lockById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "用户不存在"
                ));
    }

    private AdminUserResponse response(User user) {
        return AdminUserResponse.from(
                user,
                roleService.findRoleCodesByUserId(user.getId())
        );
    }

    private String normalize(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
