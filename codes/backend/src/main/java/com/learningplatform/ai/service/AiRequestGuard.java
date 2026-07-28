package com.learningplatform.ai.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.AiProperties;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.common.redis.RedisRequestLimiter;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Protects AI calls with rate, concurrency, and execution-time limits.
 *
 * <p>Redis provides a cross-instance sliding window and tokenized concurrency
 * leases. If Redis is unavailable, the existing local window and semaphore
 * remain active, so an infrastructure outage does not disable protection.</p>
 */
@Service
public class AiRequestGuard {
    private static final Logger log =
            LoggerFactory.getLogger(AiRequestGuard.class);

    private final int requestsPerWindow;
    private final Duration rateWindow;
    private final int maxConcurrentPerUser;
    private final Duration timeout;
    private final Map<Long, Deque<Instant>> requestWindows =
            new ConcurrentHashMap<>();
    private final Map<Long, Semaphore> userSemaphores =
            new ConcurrentHashMap<>();
    private final ExecutorService executor;
    private final RedisRequestLimiter redisRequestLimiter;

    /**
     * Constructor retained for existing unit tests and direct callers.
     */
    public AiRequestGuard(AiProperties properties) {
        this(properties, null);
    }

    @Autowired
    public AiRequestGuard(
            AiProperties properties,
            RedisRequestLimiter redisRequestLimiter
    ) {
        AiProperties.Limits limits = requireLimits(properties);
        this.requestsPerWindow = positive(
                limits.requestsPerWindow(),
                "AI 调用频率上限必须大于 0"
        );
        this.rateWindow = positive(
                limits.rateWindow(),
                "AI 限流窗口必须大于 0"
        );
        this.maxConcurrentPerUser = positive(
                limits.maxConcurrentPerUser(),
                "AI 用户并发上限必须大于 0"
        );
        this.timeout = positive(
                limits.timeout(),
                "AI 请求超时时间必须大于 0"
        );
        this.redisRequestLimiter = redisRequestLimiter;

        AtomicInteger sequence = new AtomicInteger();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "ai-request-" + sequence.incrementAndGet()
            );
            thread.setDaemon(true);
            return thread;
        };
        this.executor = Executors.newCachedThreadPool(factory);
    }

    public <T> T execute(Long userId, Supplier<T> operation) {
        GuardPermit permit = acquirePermit(userId);
        Future<T> future;
        Map<String, String> requestContext = MDC.getCopyOfContextMap();
        try {
            future = executor.submit(() -> {
                if (requestContext == null || requestContext.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(requestContext);
                }
                try {
                    return operation.get();
                } finally {
                    MDC.clear();
                    // Release only after the underlying operation actually
                    // ends. Some HTTP clients do not stop immediately when
                    // their Future is interrupted.
                    permit.release();
                }
            });
        } catch (RuntimeException exception) {
            permit.release();
            throw exception;
        }

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new GuardException(
                    GuardFailure.TIMEOUT,
                    ErrorCode.INTERNAL_ERROR,
                    "AI 请求处理超时，请稍后重试"
            );
        } catch (InterruptedException exception) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new GuardException(
                    GuardFailure.INTERRUPTED,
                    ErrorCode.INTERNAL_ERROR,
                    "AI 请求被中断，请稍后重试"
            );
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 请求处理失败，请稍后重试"
            );
        }
    }

    private GuardPermit acquirePermit(Long userId) {
        if (redisRequestLimiter != null) {
            try {
                boolean allowed = redisRequestLimiter.acquireSlidingWindow(
                        "rate:ai:user:" + userId,
                        System.currentTimeMillis(),
                        rateWindow,
                        requestsPerWindow
                );
                if (!allowed) {
                    throw rateLimitException();
                }

                // A finite lease prevents abandoned locks. The extra grace
                // period covers cancellation/HTTP-client shutdown latency.
                Duration leaseDuration = timeout
                        .multipliedBy(2)
                        .plusSeconds(30);
                RedisRequestLimiter.Lease lease =
                        redisRequestLimiter.tryAcquireLease(
                                "lease:ai:user:" + userId,
                                maxConcurrentPerUser,
                                leaseDuration
                        );
                if (lease == null) {
                    throw concurrencyException();
                }
                return () -> releaseRedisLease(lease);
            } catch (GuardException exception) {
                throw exception;
            } catch (RedisRequestLimiter.FeatureDisabledException exception) {
                // Controlled test/local mode: continue with the local guard.
            } catch (RuntimeException exception) {
                log.warn(
                        "Redis unavailable for AI guard; "
                                + "using local fallback userId={} cause={}",
                        userId,
                        exception.toString()
                );
            }
        }
        return acquireLocalPermit(userId);
    }

    private GuardPermit acquireLocalPermit(Long userId) {
        recordLocalRequest(userId);
        Semaphore semaphore = userSemaphores.computeIfAbsent(
                userId,
                ignored -> new Semaphore(maxConcurrentPerUser)
        );
        if (!semaphore.tryAcquire()) {
            throw concurrencyException();
        }
        return semaphore::release;
    }

    private void releaseRedisLease(RedisRequestLimiter.Lease lease) {
        try {
            redisRequestLimiter.releaseLease(lease);
        } catch (RuntimeException exception) {
            // Compare-and-delete normally releases the lease. If Redis is
            // down, its TTL guarantees eventual cleanup.
            log.warn(
                    "Failed to release AI Redis lease key={}; "
                            + "waiting for lease expiry cause={}",
                    lease.key(),
                    exception.toString()
            );
        }
    }

    private void recordLocalRequest(Long userId) {
        Instant now = Instant.now();
        Instant earliest = now.minus(rateWindow);
        Deque<Instant> window = requestWindows.computeIfAbsent(
                userId,
                ignored -> new ArrayDeque<>()
        );
        synchronized (window) {
            while (!window.isEmpty()
                    && !window.peekFirst().isAfter(earliest)) {
                window.removeFirst();
            }
            if (window.size() >= requestsPerWindow) {
                throw rateLimitException();
            }
            window.addLast(now);
        }
    }

    private GuardException rateLimitException() {
        return new GuardException(
                GuardFailure.RATE_LIMIT,
                ErrorCode.TOO_MANY_REQUESTS,
                "AI 调用过于频繁，请稍后重试"
        );
    }

    private GuardException concurrencyException() {
        return new GuardException(
                GuardFailure.CONCURRENCY_LIMIT,
                ErrorCode.TOO_MANY_REQUESTS,
                "当前已有 AI 请求正在处理，请稍后重试"
        );
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private AiProperties.Limits requireLimits(AiProperties properties) {
        if (properties == null || properties.limits() == null) {
            throw new IllegalStateException("缺少 AI 限制配置");
        }
        return properties.limits();
    }

    private int positive(int value, String message) {
        if (value <= 0) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    private Duration positive(Duration value, String message) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    public enum GuardFailure {
        RATE_LIMIT,
        CONCURRENCY_LIMIT,
        TIMEOUT,
        INTERRUPTED
    }

    @FunctionalInterface
    private interface GuardPermit {
        void release();
    }

    public static final class GuardException extends BusinessException {
        private final GuardFailure failure;

        public GuardException(
                GuardFailure failure,
                ErrorCode errorCode,
                String message
        ) {
            super(errorCode, message);
            this.failure = failure;
        }

        public GuardFailure getFailure() {
            return failure;
        }
    }
}
