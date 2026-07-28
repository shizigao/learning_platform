/* 文件职责：实现角色业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.auth.security.AuthSnapshotCache;
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
/**
 * 实现角色业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class RoleService {
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 访问角色持久化数据。 */
    private final RoleMapper roleMapper;
    /** 访问用户角色持久化数据。 */
    private final UserRoleMapper userRoleMapper;
    private final AuthSnapshotCache authSnapshotCache;
    private final PublicUserProfileCache publicUserProfileCache;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public RoleService(
            UserService userService,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            AuthSnapshotCache authSnapshotCache,
            PublicUserProfileCache publicUserProfileCache
    ) {
        this.userService = userService;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.authSnapshotCache = authSnapshotCache;
        this.publicUserProfileCache = publicUserProfileCache;
    }

    /** 查询Roles按用户ID相关数据；只返回当前调用方有权查看的结果。 */
    public List<Role> findRolesByUserId(Long userId) {
        return roleMapper.findEnabledByUserId(userId);
    }

    /** 查询角色Codes按用户ID相关数据；只返回当前调用方有权查看的结果。 */
    public Set<RoleCode> findRoleCodesByUserId(Long userId) {
        return findRolesByUserId(userId).stream()
                .map(Role::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    /** 执行 assignRole 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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
        boolean inserted = userRoleMapper.insert(userRole) == 1;
        if (inserted) {
            authSnapshotCache.evictAfterCommit(userId);
            publicUserProfileCache.evictAfterCommit(userId);
        }
        return inserted;
    }

    @Transactional
    /** 删除、移除或清理角色，同时维护关联数据和权限不变量。 */
    public boolean removeRole(Long userId, RoleCode roleCode) {
        Role role = getRequiredRole(roleCode);
        boolean deleted = userRoleMapper.delete(userId, role.getId()) == 1;
        if (deleted) {
            authSnapshotCache.evictAfterCommit(userId);
            publicUserProfileCache.evictAfterCommit(userId);
        }
        return deleted;
    }

    /** 返回Required角色。 */
    private Role getRequiredRole(RoleCode roleCode) {
        return roleMapper.findEnabledByCode(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "角色不存在或已停用"));
    }
}
