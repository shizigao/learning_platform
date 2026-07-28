/* 文件职责：实现管理用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：平台治理与管理员操作；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现管理用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class AdminUserService {
    /** 访问用户持久化数据。 */
    private final UserMapper userMapper;
    /** 委托用户执行对应领域规则。 */
    private final UserService userService;
    /** 委托角色执行对应领域规则。 */
    private final RoleService roleService;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public AdminUserService(
            UserMapper userMapper,
            UserService userService,
            RoleService roleService
    ) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.roleService = roleService;
    }

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
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

    /** 查询目标相关数据；只返回当前调用方有权查看的结果。 */
    public AdminUserResponse detail(Long userId) {
        return response(userService.getRequiredById(userId));
    }

    @Transactional
    /** 更新状态，通过返回值或版本条件识别并发状态变化。 */
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
    /** 更新Roles，通过返回值或版本条件识别并发状态变化。 */
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

    /** 执行 lockUser 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private void lockUser(Long userId) {
        userMapper.lockById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.NOT_FOUND,
                        "用户不存在"
                ));
    }

    /** 执行 response 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private AdminUserResponse response(User user) {
        return AdminUserResponse.from(
                user,
                roleService.findRoleCodesByUserId(user.getId())
        );
    }

    /** 转换或规范化数据，不引入额外持久化副作用。 */
    private String normalize(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}
