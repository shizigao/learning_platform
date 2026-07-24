package com.learningplatform.user.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.domain.Role;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.UserRole;
import com.learningplatform.user.mapper.RoleMapper;
import com.learningplatform.user.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {
    private final UserService userService;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(
            UserService userService,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper
    ) {
        this.userService = userService;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public List<Role> findRolesByUserId(Long userId) {
        return roleMapper.findEnabledByUserId(userId);
    }

    public Set<RoleCode> findRoleCodesByUserId(Long userId) {
        return findRolesByUserId(userId).stream()
                .map(Role::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public boolean assignRole(Long userId, RoleCode roleCode, Long grantedBy) {
        userService.getRequiredById(userId);
        Role role = getRequiredRole(roleCode);
        if (userRoleMapper.exists(userId, role.getId())) {
            return false;
        }

        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setGrantedBy(grantedBy);
        return userRoleMapper.insert(userRole) == 1;
    }

    @Transactional
    public boolean removeRole(Long userId, RoleCode roleCode) {
        Role role = getRequiredRole(roleCode);
        return userRoleMapper.delete(userId, role.getId()) == 1;
    }

    private Role getRequiredRole(RoleCode roleCode) {
        return roleMapper.findEnabledByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在或已停用"));
    }
}
