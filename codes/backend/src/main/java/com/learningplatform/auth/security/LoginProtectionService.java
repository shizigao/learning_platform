package com.learningplatform.auth.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.LoginProtectionProperties;
import com.learningplatform.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class LoginProtectionService {
    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);
    private static final String KEY_PREFIX = "auth:login:";

    private final StringRedisTemplate redisTemplate;
    private final LoginProtectionProperties properties;

    public LoginProtectionService(
            StringRedisTemplate redisTemplate,
            LoginProtectionProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void checkAllowed(String username, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        try {
            if (isBlocked(accountBlockKey(username)) || isBlocked(ipBlockKey(ipAddress))) {
                throw tooManyAttempts();
            }
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while checking login protection; allowing this attempt");
        }
    }

    public void recordFailure(String username, String ipAddress) {
        if (!properties.enabled()) {
            return;
        }
        try {
            boolean accountBlocked = increment(
                    accountFailureKey(username),
                    accountBlockKey(username),
                    properties.maxAccountFailures()
            );
            boolean ipBlocked = increment(
                    ipFailureKey(ipAddress),
                    ipBlockKey(ipAddress),
                    properties.maxIpFailures()
            );
            if (accountBlocked || ipBlocked) {
                throw tooManyAttempts();
            }
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while recording a failed login attempt");
        }
    }

    public void recordSuccess(String username) {
        if (!properties.enabled()) {
            return;
        }
        try {
            redisTemplate.delete(List.of(
                    accountFailureKey(username),
                    accountBlockKey(username)
            ));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while clearing login protection state");
        }
    }

    private boolean increment(String failureKey, String blockKey, int limit) {
        Long failures = redisTemplate.opsForValue().increment(failureKey);
        if (failures == null) {
            return false;
        }
        if (failures == 1L) {
            redisTemplate.expire(failureKey, properties.window());
        }
        if (failures >= limit) {
            redisTemplate.opsForValue().set(blockKey, "1", properties.window());
            return true;
        }
        return false;
    }

    private boolean isBlocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private BusinessException tooManyAttempts() {
        return new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "登录失败次数过多，请稍后重试"
        );
    }

    private String accountFailureKey(String username) {
        return KEY_PREFIX + "failure:account:" + digest(username);
    }

    private String accountBlockKey(String username) {
        return KEY_PREFIX + "block:account:" + digest(username);
    }

    private String ipFailureKey(String ipAddress) {
        return KEY_PREFIX + "failure:ip:" + digest(ipAddress);
    }

    private String ipBlockKey(String ipAddress) {
        return KEY_PREFIX + "block:ip:" + digest(ipAddress);
    }

    private String digest(String value) {
        String safeValue = value == null ? "" : value;
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(safeValue.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
