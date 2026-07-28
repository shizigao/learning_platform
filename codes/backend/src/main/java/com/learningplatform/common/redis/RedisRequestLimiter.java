package com.learningplatform.common.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis atomic primitives used by distributed request guards.
 *
 * <p>Redis failures deliberately escape this class. Callers decide how to
 * degrade safely when Redis is unavailable.</p>
 */
@Component
public class RedisRequestLimiter {
    private static final DefaultRedisScript<List> FIXED_WINDOW_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])
                    if count == 1 then
                      redis.call('PEXPIRE', KEYS[1], ARGV[1])
                    end
                    local ttl = redis.call('PTTL', KEYS[1])
                    return {count, ttl}
                    """,
                    List.class
            );
    private static final DefaultRedisScript<Long> SLIDING_WINDOW_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
                    local count = redis.call('ZCARD', KEYS[1])
                    if count >= tonumber(ARGV[2]) then
                      return 0
                    end
                    redis.call('ZADD', KEYS[1], ARGV[3], ARGV[4])
                    redis.call('PEXPIRE', KEYS[1], ARGV[5])
                    return 1
                    """,
                    Long.class
            );
    private static final DefaultRedisScript<Long> COMPARE_DELETE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                      return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public RedisRequestLimiter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, true);
    }

    @Autowired
    public RedisRequestLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${app.redis.distributed-guards-enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Acquires one request in an aligned fixed window.
     */
    public FixedWindowDecision acquireFixedWindow(
            String keyPrefix,
            long nowMillis,
            long windowMillis,
            int limit
    ) {
        requireEnabled();
        long bucket = Math.floorDiv(nowMillis, windowMillis);
        long remainingMillis = windowMillis
                - Math.floorMod(nowMillis, windowMillis);
        List<?> result = redisTemplate.execute(
                FIXED_WINDOW_SCRIPT,
                List.of(keyPrefix + ':' + bucket),
                Long.toString(remainingMillis)
        );
        if (result == null || result.size() < 2) {
            throw new IllegalStateException(
                    "Redis fixed-window script returned no result"
            );
        }
        long count = number(result.get(0));
        long ttlMillis = Math.max(1L, number(result.get(1)));
        return new FixedWindowDecision(
                count <= limit,
                Math.max(1L, (ttlMillis + 999L) / 1_000L)
        );
    }

    /**
     * Atomically records a request in a sliding window.
     */
    public boolean acquireSlidingWindow(
            String key,
            long nowMillis,
            Duration window,
            int limit
    ) {
        requireEnabled();
        long windowMillis = window.toMillis();
        Long result = redisTemplate.execute(
                SLIDING_WINDOW_SCRIPT,
                List.of(key),
                Long.toString(nowMillis - windowMillis),
                Integer.toString(limit),
                Long.toString(nowMillis),
                nowMillis + "-" + UUID.randomUUID(),
                Long.toString(windowMillis)
        );
        if (result == null) {
            throw new IllegalStateException(
                    "Redis sliding-window script returned no result"
            );
        }
        return result == 1L;
    }

    /**
     * Acquires one tokenized distributed lease slot using SET NX PX.
     */
    public Lease tryAcquireLease(
            String keyPrefix,
            int maxConcurrent,
            Duration leaseDuration
    ) {
        requireEnabled();
        String token = UUID.randomUUID().toString();
        for (int slot = 0; slot < maxConcurrent; slot++) {
            String key = keyPrefix + ":slot:" + slot;
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                    key,
                    token,
                    leaseDuration
            );
            if (Boolean.TRUE.equals(acquired)) {
                return new Lease(key, token);
            }
        }
        return null;
    }

    /**
     * Releases the lease only when the caller still owns its token.
     */
    public boolean releaseLease(Lease lease) {
        requireEnabled();
        Long deleted = redisTemplate.execute(
                COMPARE_DELETE_SCRIPT,
                List.of(lease.key()),
                lease.token()
        );
        return deleted != null && deleted == 1L;
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new FeatureDisabledException();
        }
    }

    public record FixedWindowDecision(
            boolean allowed,
            long retryAfterSeconds
    ) {
    }

    public record Lease(String key, String token) {
    }

    public static final class FeatureDisabledException extends RuntimeException {
        public FeatureDisabledException() {
            super("Redis distributed guards are disabled");
        }
    }
}
