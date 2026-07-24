package com.learningplatform.ai.service;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.AiProperties;
import com.learningplatform.common.exception.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.MDC;
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

@Service
public class AiRequestGuard {
    private final int requestsPerWindow;
    private final Duration rateWindow;
    private final int maxConcurrentPerUser;
    private final Duration timeout;
    private final Map<Long, Deque<Instant>> requestWindows = new ConcurrentHashMap<>();
    private final Map<Long, Semaphore> userSemaphores = new ConcurrentHashMap<>();
    private final ExecutorService executor;

    public AiRequestGuard(AiProperties properties) {
        AiProperties.Limits limits = requireLimits(properties);
        this.requestsPerWindow = positive(
                limits.requestsPerWindow(),
                "AI 调用频率上限必须大于0"
        );
        this.rateWindow = positive(limits.rateWindow(), "AI 限流窗口必须大于0");
        this.maxConcurrentPerUser = positive(
                limits.maxConcurrentPerUser(),
                "AI 用户并发上限必须大于0"
        );
        this.timeout = positive(limits.timeout(), "AI 请求超时时间必须大于0");
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
        recordRequest(userId);
        Semaphore semaphore = userSemaphores.computeIfAbsent(
                userId,
                ignored -> new Semaphore(maxConcurrentPerUser)
        );
        if (!semaphore.tryAcquire()) {
            throw new GuardException(
                    GuardFailure.CONCURRENCY_LIMIT,
                    ErrorCode.TOO_MANY_REQUESTS,
                    "当前已有 AI 请求正在处理，请稍后重试"
            );
        }
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
                    // 部分 HTTP 客户端不会立刻响应线程中断。只有底层操作真正结束后
                    // 才允许同一用户发起下一次调用，避免超时请求与重试请求重叠。
                    semaphore.release();
                }
            });
        } catch (RuntimeException exception) {
            semaphore.release();
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

    private void recordRequest(Long userId) {
        Instant now = Instant.now();
        Instant earliest = now.minus(rateWindow);
        Deque<Instant> window = requestWindows.computeIfAbsent(
                userId,
                ignored -> new ArrayDeque<>()
        );
        synchronized (window) {
            while (!window.isEmpty() && !window.peekFirst().isAfter(earliest)) {
                window.removeFirst();
            }
            if (window.size() >= requestsPerWindow) {
                throw new GuardException(
                        GuardFailure.RATE_LIMIT,
                        ErrorCode.TOO_MANY_REQUESTS,
                        "AI 调用过于频繁，请稍后重试"
                );
            }
            window.addLast(now);
        }
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
