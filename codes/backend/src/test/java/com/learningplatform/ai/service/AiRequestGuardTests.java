package com.learningplatform.ai.service;

import com.learningplatform.common.config.AiProperties;
import com.learningplatform.common.redis.RedisRequestLimiter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiRequestGuardTests {
    private AiRequestGuard guard;

    @AfterEach
    void tearDown() {
        MDC.clear();
        if (guard != null) {
            guard.shutdown();
        }
    }

    @Test
    void propagatesTraceContextToAiWorkerWithoutLeakingIt() {
        guard = new AiRequestGuard(properties(
                10,
                1,
                Duration.ofMinutes(1),
                Duration.ofSeconds(1)
        ));
        MDC.put("traceId", "trace-for-ai-call");

        assertThat(guard.execute(8L, () -> MDC.get("traceId")))
                .isEqualTo("trace-for-ai-call");

        MDC.clear();
        String leakedTraceId = guard.execute(9L, () -> MDC.get("traceId"));
        assertThat(leakedTraceId).isNull();
    }

    @Test
    void rejectsCallsBeyondRateWindow() {
        guard = new AiRequestGuard(properties(
                1,
                1,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        ));

        assertThat(guard.execute(1L, () -> "ok")).isEqualTo("ok");
        assertThatThrownBy(() -> guard.execute(1L, () -> "again"))
                .isInstanceOfSatisfying(
                        AiRequestGuard.GuardException.class,
                        exception -> assertThat(exception.getFailure())
                                .isEqualTo(
                                        AiRequestGuard.GuardFailure.RATE_LIMIT
                                )
                );
    }

    @Test
    void rejectsConcurrentCallForSameUser() throws Exception {
        guard = new AiRequestGuard(properties(
                10,
                1,
                Duration.ofMinutes(1),
                Duration.ofSeconds(2)
        ));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            Future<String> first = caller.submit(() -> guard.execute(2L, () -> {
                started.countDown();
                try {
                    release.await(1, TimeUnit.SECONDS);
                    return "done";
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            assertThat(started.await(1, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> guard.execute(2L, () -> "second"))
                    .isInstanceOfSatisfying(
                            AiRequestGuard.GuardException.class,
                            exception -> assertThat(exception.getFailure())
                                    .isEqualTo(
                                            AiRequestGuard.GuardFailure
                                                    .CONCURRENCY_LIMIT
                                    )
                    );
            release.countDown();
            assertThat(first.get(1, TimeUnit.SECONDS)).isEqualTo("done");
        } finally {
            release.countDown();
            caller.shutdownNow();
        }
    }

    @Test
    void cancelsCallAfterConfiguredTimeout() {
        guard = new AiRequestGuard(properties(
                10,
                1,
                Duration.ofMinutes(1),
                Duration.ofMillis(30)
        ));

        assertThatThrownBy(() -> guard.execute(3L, () -> {
            try {
                Thread.sleep(500);
                return "late";
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        })).isInstanceOfSatisfying(
                AiRequestGuard.GuardException.class,
                exception -> assertThat(exception.getFailure())
                        .isEqualTo(AiRequestGuard.GuardFailure.TIMEOUT)
        );
    }

    @Test
    void retainsConcurrencyPermitUntilTimedOutOperationActuallyStops()
            throws Exception {
        guard = new AiRequestGuard(properties(
                10,
                1,
                Duration.ofMinutes(1),
                Duration.ofMillis(30)
        ));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch stopped = new CountDownLatch(1);

        assertThatThrownBy(() -> guard.execute(4L, () -> {
            started.countDown();
            try {
                while (!release.await(20, TimeUnit.MILLISECONDS)) {
                    // 模拟不响应 interrupt、仍占用网络连接的底层客户端。
                }
                return "late";
            } catch (InterruptedException ignored) {
                try {
                    release.await(1, TimeUnit.SECONDS);
                    return "late";
                } catch (InterruptedException secondInterrupt) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(secondInterrupt);
                }
            } finally {
                stopped.countDown();
            }
        })).isInstanceOfSatisfying(
                AiRequestGuard.GuardException.class,
                exception -> assertThat(exception.getFailure())
                        .isEqualTo(AiRequestGuard.GuardFailure.TIMEOUT)
        );
        assertThat(started.getCount()).isZero();

        assertThatThrownBy(() -> guard.execute(4L, () -> "overlap"))
                .isInstanceOfSatisfying(
                        AiRequestGuard.GuardException.class,
                        exception -> assertThat(exception.getFailure())
                                .isEqualTo(
                                        AiRequestGuard.GuardFailure
                                                .CONCURRENCY_LIMIT
                                )
                );

        release.countDown();
        assertThat(stopped.await(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void usesRedisSlidingWindowAndReleasesOwnedLease() {
        RedisRequestLimiter limiter = mock(RedisRequestLimiter.class);
        RedisRequestLimiter.Lease lease =
                new RedisRequestLimiter.Lease("lease-key", "owner-token");
        when(limiter.acquireSlidingWindow(
                anyString(),
                anyLong(),
                any(Duration.class),
                anyInt()
        )).thenReturn(true);
        when(limiter.tryAcquireLease(
                anyString(),
                anyInt(),
                any(Duration.class)
        )).thenReturn(lease);
        guard = new AiRequestGuard(properties(
                10,
                1,
                Duration.ofMinutes(1),
                Duration.ofSeconds(1)
        ), limiter);

        assertThat(guard.execute(10L, () -> "ok")).isEqualTo("ok");

        verify(limiter).releaseLease(lease);
    }

    @Test
    void rejectsWhenRedisSlidingWindowIsFull() {
        RedisRequestLimiter limiter = mock(RedisRequestLimiter.class);
        when(limiter.acquireSlidingWindow(
                anyString(),
                anyLong(),
                any(Duration.class),
                anyInt()
        )).thenReturn(false);
        guard = new AiRequestGuard(properties(
                1,
                1,
                Duration.ofMinutes(1),
                Duration.ofSeconds(1)
        ), limiter);

        assertThatThrownBy(() -> guard.execute(11L, () -> "blocked"))
                .isInstanceOfSatisfying(
                        AiRequestGuard.GuardException.class,
                        exception -> assertThat(exception.getFailure())
                                .isEqualTo(
                                        AiRequestGuard.GuardFailure.RATE_LIMIT
                                )
                );
    }

    @Test
    void fallsBackToLocalGuardWhenRedisFails() {
        RedisRequestLimiter limiter = mock(RedisRequestLimiter.class);
        when(limiter.acquireSlidingWindow(
                anyString(),
                anyLong(),
                any(Duration.class),
                anyInt()
        )).thenThrow(new IllegalStateException("redis unavailable"));
        guard = new AiRequestGuard(properties(
                1,
                1,
                Duration.ofMinutes(1),
                Duration.ofSeconds(1)
        ), limiter);

        assertThat(guard.execute(12L, () -> "ok")).isEqualTo("ok");
        assertThatThrownBy(() -> guard.execute(12L, () -> "again"))
                .isInstanceOfSatisfying(
                        AiRequestGuard.GuardException.class,
                        exception -> assertThat(exception.getFailure())
                                .isEqualTo(
                                        AiRequestGuard.GuardFailure.RATE_LIMIT
                                )
                );
    }

    private AiProperties properties(
            int requests,
            int concurrency,
            Duration window,
            Duration timeout
    ) {
        return new AiProperties(
                "mock",
                new AiProperties.MockProvider("test", "success", Duration.ZERO),
                null,
                new AiProperties.Limits(
                        100_000,
                        10,
                        20_000,
                        requests,
                        window,
                        concurrency,
                        timeout
                )
        );
    }
}
