package com.learningplatform.common.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 统一的 JSON Redis 旁路缓存。
 *
 * <p>该组件从不把 Redis 当作业务真源。缓存读取、反序列化或写入失败时，调用方
 * 仍会执行数据库 loader；写操作可使用 {@link #evictAfterCommit(String)} 保证
 * 只在事务真正提交后失效共享缓存。</p>
 */
@Component
public class RedisJsonCache {
    private static final Logger log = LoggerFactory.getLogger(RedisJsonCache.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public RedisJsonCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.read-cache-enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public <T> T get(
            String key,
            TypeReference<T> type,
            Duration ttl,
            Supplier<T> loader
    ) {
        if (!enabled) {
            return loader.get();
        }
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, type);
            }
        } catch (DataAccessException | JsonProcessingException exception) {
            log.warn("Redis cache read failed key={}; falling back to database", key);
        }

        T loaded = loader.get();
        if (loaded != null) {
            try {
                redisTemplate.opsForValue().set(
                        key,
                        objectMapper.writeValueAsString(loaded),
                        ttl
                );
            } catch (DataAccessException | JsonProcessingException exception) {
                log.warn("Redis cache write failed key={}", key);
            }
        }
        return loaded;
    }

    public void evictAfterCommit(String key) {
        if (!enabled) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            evict(key);
                        }
                    }
            );
            return;
        }
        evict(key);
    }

    public void evict(String key) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            log.warn("Redis cache eviction failed key={}", key);
        }
    }
}
