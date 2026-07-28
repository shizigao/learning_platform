/* 文件职责：实现LoginProtection业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：身份认证、JWT 与登录安全；所在分层：安全认证层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
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
/**
 * 实现LoginProtection业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：建立请求身份与安全上下文，资源级权限仍由领域服务校验。</p>
 */
public class LoginProtectionService {
    private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);
    /** 定义 KEY_PREFIX 常量，统一该组件使用的固定规则或默认值。 */
    private static final String KEY_PREFIX = "auth:login:";

    /** 保存redis模板，供该类型的业务逻辑读取或更新。 */
    private final StringRedisTemplate redisTemplate;
    /** 保存配置属性，供该类型的业务逻辑读取或更新。 */
    private final LoginProtectionProperties properties;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public LoginProtectionService(
            StringRedisTemplate redisTemplate,
            LoginProtectionProperties properties
    ) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /** 校验Allowed及相关业务前置条件，不满足时抛出明确业务异常。 */
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

    /** 执行 recordFailure 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 执行 recordSuccess 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    public void recordSuccess(String username) {
        if (!properties.enabled()) {
            return;
        }
        try {
            // redisTemplate，此处使用了redis
            redisTemplate.delete(List.of(
                    accountFailureKey(username),
                    accountBlockKey(username)
            ));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while clearing login protection state");
        }
    }

    /** 执行 increment 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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

    /** 判断是否满足Blocked条件，不修改持久化状态。 */
    private boolean isBlocked(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /** 转换或规范化oManyAttempts数据，不引入额外持久化副作用。 */
    private BusinessException tooManyAttempts() {
        return new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "登录失败次数过多，请稍后重试"
        );
    }

    /** 执行 accountFailureKey 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String accountFailureKey(String username) {
        return KEY_PREFIX + "failure:account:" + digest(username);
    }

    /** 执行 accountBlockKey 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String accountBlockKey(String username) {
        return KEY_PREFIX + "block:account:" + digest(username);
    }

    /** 执行 ipFailureKey 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String ipFailureKey(String ipAddress) {
        return KEY_PREFIX + "failure:ip:" + digest(ipAddress);
    }

    /** 执行 ipBlockKey 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
    private String ipBlockKey(String ipAddress) {
        return KEY_PREFIX + "block:ip:" + digest(ipAddress);
    }

    /** 执行 digest 对应职责；具体输入输出由方法签名和所属类型共同约束。 */
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
