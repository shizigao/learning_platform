package com.learningplatform.common.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationDecisionCacheTests {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> values;

    @Test
    void cachedDecisionAvoidsDatabaseLookup() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.contains(":decision:") ? "1" : "0";
        });
        AtomicInteger loads = new AtomicInteger();

        boolean allowed = cache().contentClassAccess(
                3L,
                9L,
                () -> {
                    loads.incrementAndGet();
                    return false;
                }
        );

        assertThat(allowed).isTrue();
        assertThat(loads).hasValue(0);
    }

    @Test
    void redisFailureNeverGrantsAndFallsBackToDatabase() {
        when(redisTemplate.opsForValue()).thenReturn(values);
        when(values.get(anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThat(cache().contentEntitlement(3L, 9L, () -> false)).isFalse();
    }

    @Test
    void userMutationAdvancesSharedAuthorizationVersion() {
        when(redisTemplate.opsForValue()).thenReturn(values);

        cache().bumpUserAfterCommit(3L);

        verify(values).increment("lp:v1:authz:version:user:3");
    }

    private AuthorizationDecisionCache cache() {
        return new AuthorizationDecisionCache(
                redisTemplate,
                true,
                Duration.ofSeconds(30)
        );
    }
}
