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
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Optional<User> findById(Long userId) {
        return userMapper.findById(userId);
    }

    public User getRequiredById(Long userId) {
        return findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    }

    public User getRequiredActiveById(Long userId) {
        User user = getRequiredById(userId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在或当前不可访问");
        }
        return user;
    }

    public Optional<User> findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userMapper.existsByUsername(username);
    }

    public List<User> searchActive(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userMapper.searchActive(normalized);
    }

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

    public boolean emailExists(String email, Long excludedUserId) {
        return email != null && userMapper.existsByEmail(email, excludedUserId);
    }

    public boolean phoneExists(String phone, Long excludedUserId) {
        return phone != null && userMapper.existsByPhone(phone, excludedUserId);
    }

    @Transactional
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
    public void updateProfile(User user) {
        if (userMapper.updateProfile(user) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    public void clearAvatarUrl(Long userId) {
        if (userMapper.clearAvatarUrl(userId) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    public void updatePasswordHash(Long userId, String passwordHash) {
        if (userMapper.updatePasswordHash(userId, passwordHash) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    public void recordLogin(Long userId, String loginIp) {
        if (userMapper.updateLastLogin(userId, LocalDateTime.now(), loginIp) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }

    @Transactional
    public void updateStatus(Long userId, UserStatus status) {
        if (userMapper.updateStatus(userId, status) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
    }
}
