package com.learningplatform.exam.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learningplatform.common.redis.RedisJsonCache;
import com.learningplatform.exam.dto.ExamStatisticsResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/** Redis-backed short-lived cache for expensive exam aggregate statistics. */
@Component
public class ExamStatisticsCache {
    private static final TypeReference<ExamStatisticsResponse> TYPE =
            new TypeReference<>() {
            };
    private final RedisJsonCache redisJsonCache;

    public ExamStatisticsCache(RedisJsonCache redisJsonCache) {
        this.redisJsonCache = redisJsonCache;
    }

    public ExamStatisticsResponse get(
            Long examId,
            Supplier<ExamStatisticsResponse> loader
    ) {
        return redisJsonCache.get(
                key(examId),
                TYPE,
                Duration.ofSeconds(20),
                loader
        );
    }

    public void evictAfterCommit(Long examId) {
        redisJsonCache.evictAfterCommit(key(examId));
    }

    private String key(Long examId) {
        return "lp:v1:exam:statistics:" + examId;
    }
}
