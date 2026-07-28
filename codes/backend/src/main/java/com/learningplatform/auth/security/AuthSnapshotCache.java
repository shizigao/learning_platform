package com.learningplatform.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 缓存 JWT 认证所需的最小用户快照，避免每个受保护请求都查询用户表和角色表。
 *
 * <p>Redis 仅是加速层：读取或写入失败时立即回源数据库。账号状态或角色变化会在
 * 当前事务提交后主动失效，60 秒 TTL 只作为异常情况下的安全兜底。</p>
 */
@Component
public class AuthSnapshotCache {
    private static final Logger log = LoggerFactory.getLogger(AuthSnapshotCache.class);
    private static final String KEY_PREFIX = "lp:v1:auth:snapshot:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;

    public AuthSnapshotCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.auth-snapshot-enabled:true}") boolean enabled,
            @Value("${app.redis.auth-snapshot-ttl:60s}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttl = ttl;
    }

    public Snapshot getOrLoad(Long userId, Supplier<Snapshot> loader) {
        if (!enabled) {
            return loader.get();
        }
        Snapshot cached = read(userId);
        if (cached != null) {
            return cached;
        }
        Snapshot loaded = loader.get();
        write(userId, loaded);
        return loaded;
    }

    /**
     * 在事务成功提交后删除快照，避免数据库回滚却提前删除；非事务调用立即删除。
     */
    public void evictAfterCommit(Long userId) {
        if (!enabled || userId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            evict(userId);
                        }
                    }
            );
            return;
        }
        evict(userId);
    }

    public void evict(Long userId) {
        if (!enabled || userId == null) {
            return;
        }
        try {
            redisTemplate.delete(key(userId));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while evicting authentication snapshot userId={}", userId);
        }
    }

    private Snapshot read(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(key(userId));
            return json == null ? null : objectMapper.readValue(json, Snapshot.class);
        } catch (DataAccessException | JsonProcessingException exception) {
            log.warn("Unable to read authentication snapshot userId={}; falling back to database",
                    userId);
            return null;
        }
    }

    private void write(Long userId, Snapshot snapshot) {
        try {
            redisTemplate.opsForValue().set(
                    key(userId),
                    objectMapper.writeValueAsString(snapshot),
                    ttl
            );
        } catch (DataAccessException | JsonProcessingException exception) {
            log.warn("Unable to cache authentication snapshot userId={}", userId);
        }
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    public record Snapshot(
            Long userId,
            String username,
            UserStatus status,
            Set<RoleCode> roles
    ) {
        public Snapshot {
            roles = roles == null ? Set.of() : Set.copyOf(roles);
        }
    }
}
