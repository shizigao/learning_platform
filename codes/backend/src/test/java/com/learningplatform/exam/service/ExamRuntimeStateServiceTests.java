package com.learningplatform.exam.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamRuntimeStateServiceTests {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Test
    void startingAttemptAddsDeadlineToSortedSet() {
        ExamRuntimeStateService service = service(true);
        LocalDateTime now = LocalDateTime.of(2026, 7, 28, 10, 0);
        LocalDateTime deadline = now.plusMinutes(30);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.rememberStarted(42L, deadline, now);

        double expectedScore = deadline.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        verify(zSetOperations).add(
                ExamRuntimeStateService.DEADLINE_INDEX_KEY,
                "42",
                expectedScore
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void claimsDueAttemptsThroughAtomicRedisScript() {
        ExamRuntimeStateService service = service(true);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any()
        )).thenReturn(List.of("42", "43"));

        ExamRuntimeStateService.ExpiredAttemptClaim claim =
                service.claimExpired(LocalDateTime.now(), 100);

        assertThat(claim.redisAvailable()).isTrue();
        assertThat(claim.attemptIds()).containsExactly(42L, 43L);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void reportsRedisUnavailableSoSchedulerCanUseMysql() {
        ExamRuntimeStateService service = service(true);
        when(redisTemplate.execute(
                any(RedisScript.class),
                anyList(),
                any(),
                any()
        )).thenThrow(new RedisConnectionFailureException("redis unavailable"));

        ExamRuntimeStateService.ExpiredAttemptClaim claim =
                service.claimExpired(LocalDateTime.now(), 100);

        assertThat(claim.redisAvailable()).isFalse();
        assertThat(claim.attemptIds()).isEmpty();
    }

    @Test
    void clearingAttemptRemovesDeadlineIndexAndDetailKeys() {
        ExamRuntimeStateService service = service(true);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        service.clear(42L);

        verify(zSetOperations).remove(ExamRuntimeStateService.DEADLINE_INDEX_KEY, "42");
        verify(redisTemplate).delete(eq(List.of(
                "exam:attempt:42:deadline",
                "exam:attempt:42:last-saved"
        )));
    }

    @Test
    void disabledRuntimeStateDoesNotUseRedis() {
        ExamRuntimeStateService service = service(false);

        service.rememberStarted(42L, LocalDateTime.now().plusMinutes(1), LocalDateTime.now());
        ExamRuntimeStateService.ExpiredAttemptClaim claim =
                service.claimExpired(LocalDateTime.now(), 100);
        service.requeueExpired(42L, LocalDateTime.now().plusSeconds(5));
        service.clear(42L);

        assertThat(claim.redisAvailable()).isFalse();
        verifyNoInteractions(redisTemplate);
    }

    private ExamRuntimeStateService service(boolean enabled) {
        return new ExamRuntimeStateService(redisTemplate, enabled);
    }
}
