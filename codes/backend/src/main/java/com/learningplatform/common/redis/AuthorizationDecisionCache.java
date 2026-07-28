package com.learningplatform.common.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * 以“用户版本 + 资源版本”缓存短期授权判断。
 *
 * <p>成员、权益或资源范围发生变化时只需递增对应版本，新请求便不会复用旧判断，
 * 无需扫描或通配删除 Redis Key。Redis 故障时始终回源数据库，绝不放行未知权限。</p>
 */
@Component
public class AuthorizationDecisionCache {
    private static final Logger log =
            LoggerFactory.getLogger(AuthorizationDecisionCache.class);
    private static final String PREFIX = "lp:v1:authz:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;
    private final Duration ttl;

    public AuthorizationDecisionCache(
            StringRedisTemplate redisTemplate,
            @Value("${app.redis.authorization-cache-enabled:true}") boolean enabled,
            @Value("${app.redis.authorization-cache-ttl:30s}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
        this.ttl = ttl;
    }

    public boolean contentClassAccess(
            Long userId,
            Long contentId,
            BooleanSupplier databaseLoader
    ) {
        return decide("content-class", userId, contentId, databaseLoader);
    }

    public boolean contentEntitlement(
            Long userId,
            Long contentId,
            BooleanSupplier databaseLoader
    ) {
        return decide("content-entitlement", userId, contentId, databaseLoader);
    }

    public boolean examClassAccess(
            Long userId,
            Long examId,
            BooleanSupplier databaseLoader
    ) {
        return decide("exam-class", userId, examId, databaseLoader);
    }

    public void bumpUserAfterCommit(Long userId) {
        afterCommit(() -> increment(versionUserKey(userId)));
    }

    public void bumpContentAfterCommit(Long contentId) {
        afterCommit(() -> increment(versionResourceKey("content", contentId)));
    }

    public void bumpExamAfterCommit(Long examId) {
        afterCommit(() -> increment(versionResourceKey("exam", examId)));
    }

    private boolean decide(
            String type,
            Long userId,
            Long resourceId,
            BooleanSupplier databaseLoader
    ) {
        if (!enabled || userId == null || resourceId == null) {
            return databaseLoader.getAsBoolean();
        }
        try {
            long userVersion = version(versionUserKey(userId));
            String resourceType = type.startsWith("exam") ? "exam" : "content";
            long resourceVersion = version(
                    versionResourceKey(resourceType, resourceId)
            );
            String decisionKey = PREFIX + "decision:" + type + ":u:" + userId
                    + ":uv:" + userVersion + ":r:" + resourceId
                    + ":rv:" + resourceVersion;
            String cached = redisTemplate.opsForValue().get(decisionKey);
            if (cached != null) {
                return "1".equals(cached);
            }
            boolean allowed = databaseLoader.getAsBoolean();
            redisTemplate.opsForValue().set(
                    decisionKey,
                    allowed ? "1" : "0",
                    ttl
            );
            return allowed;
        } catch (DataAccessException exception) {
            log.warn("Redis authorization cache unavailable type={} userId={} resourceId={}",
                    type, userId, resourceId);
            return databaseLoader.getAsBoolean();
        }
    }

    private long version(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private void increment(String key) {
        if (!enabled || key == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().increment(key);
        } catch (DataAccessException exception) {
            log.warn("Unable to increment Redis authorization version key={}", key);
        }
    }

    private void afterCommit(Runnable action) {
        if (!enabled) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    }
            );
            return;
        }
        action.run();
    }

    private String versionUserKey(Long userId) {
        return userId == null ? null : PREFIX + "version:user:" + userId;
    }

    private String versionResourceKey(String type, Long resourceId) {
        return resourceId == null ? null : PREFIX + "version:" + type + ":" + resourceId;
    }
}
