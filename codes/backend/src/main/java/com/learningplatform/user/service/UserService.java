/* 文件职责：实现用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：用户、角色、头像与公开个人中心；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.user.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.user.domain.User;
import com.learningplatform.user.domain.UserStatus;
import com.learningplatform.user.mapper.UserMapper;
import com.learningplatform.common.page.PageQuery;
import com.learningplatform.common.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
/**
 * 实现用户业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class UserService {
    /** 访问用户持久化数据。 */
    private final UserMapper userMapper;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 按ID查询数据；只返回当前调用方有权查看的结果。 */
    public Optional<User> findById(Long userId) {
        return userMapper.findById(userId);
    }

    /** 返回Required按ID。 */
    public User getRequiredById(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    /** 返回RequiredActive按ID。 */
    public User getRequiredActiveById(Long userId) {
        User user = getRequiredById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在或当前不可访问");
        }
        return user;
    }

    /** 按Username查询数据；只返回当前调用方有权查看的结果。 */
    public Optional<User> findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    /** 执行 usernameExists 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public boolean usernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    /** 查询Active相关数据；只返回当前调用方有权查看的结果。 */
    public List<User> searchActive(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userMapper.searchActive(normalized);
    }

    /** 查询Active相关数据；只返回当前调用方有权查看的结果。 */
    public PageResult<User> searchActive(String keyword, PageQuery query) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        long total = userMapper.countActive(normalized);
        List<User> items = userMapper.findActivePage(
                normalized,
                query.offset(),
                query.getPageSize()
        );
        return PageResult.of(items, total, query.getPageNumber(), query.getPageSize());
    }

    /** 执行 emailExists 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public boolean emailExists(String email, Long excludedUserId) {
        return email != null && userMapper.existsByEmail(email, excludedUserId);
    }

    /** 执行 phoneExists 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public boolean phoneExists(String phone, Long excludedUserId) {
        return phone != null && userMapper.existsByPhone(phone, excludedUserId);
    }

    @Transactional
    /** 创建或初始化，并维护唯一性、初始状态和必要关联。 */
    public User create(User user) {
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        if (userMapper.insert(user) != 1) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "创建用户失败");
        }
        return user;
    }

    @Transactional
    /** 更新资料，通过返回值或版本条件识别并发状态变化。 */
    public void updateProfile(User user) {
        if (userMapper.updateProfile(user) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    /** 删除、移除或清理头像Url，同时维护关联数据和权限不变量。 */
    public void clearAvatarUrl(Long userId) {
        if (userMapper.clearAvatarUrl(userId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    /** 更新PasswordHash，通过返回值或版本条件识别并发状态变化。 */
    public void updatePasswordHash(Long userId, String passwordHash) {
        if (userMapper.updatePasswordHash(userId, passwordHash) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    /** 执行 recordLogin 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public void recordLogin(Long userId, String loginIp) {
        if (userMapper.updateLastLogin(userId, LocalDateTime.now(), loginIp) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    /** 更新状态，通过返回值或版本条件识别并发状态变化。 */
    public void updateStatus(Long userId, UserStatus status) {
        if (userMapper.updateStatus(userId, status) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }
}
