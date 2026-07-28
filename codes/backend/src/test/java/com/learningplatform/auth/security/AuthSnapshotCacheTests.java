package com.learningplatform.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningplatform.user.domain.RoleCode;
import com.learningplatform.user.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSnapshotCacheTests {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> values;

    @Test
    void loadsDatabaseOnMissAndWritesShortLivedSnapshot() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(null);
        AuthSnapshotCache cache = cache(true);
        AuthSnapshotCache.Snapshot expected = snapshot();

        assertThat(cache.getOrLoad(7L, () -> expected)).isEqualTo(expected);

        verify(values).set(
                eq("lp:v1:auth:snapshot:7"),
                anyString(),
                eq(Duration.ofSeconds(60))
        );
    }

    @Test
    void redisFailureFallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));
        AtomicInteger loads = new AtomicInteger();

        AuthSnapshotCache.Snapshot result = cache(true).getOrLoad(
                7L,
                () -> {
                    loads.incrementAndGet();
                    return snapshot();
                }
        );

        assertThat(result).isEqualTo(snapshot());
        assertThat(loads).hasValue(1);
    }

    private AuthSnapshotCache cache(boolean enabled) {
        return new AuthSnapshotCache(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                enabled,
                Duration.ofSeconds(60)
        );
    }

    private AuthSnapshotCache.Snapshot snapshot() {
        return new AuthSnapshotCache.Snapshot(
                7L,
                "alice",
                UserStatus.ACTIVE,
                Set.of(RoleCode.USER)
        );
    }
}
