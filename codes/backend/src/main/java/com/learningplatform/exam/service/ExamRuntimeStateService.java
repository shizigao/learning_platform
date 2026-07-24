package com.learningplatform.exam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ExamRuntimeStateService {
    private static final Logger log = LoggerFactory.getLogger(ExamRuntimeStateService.class);
    private static final String PREFIX = "exam:attempt:";

    private final StringRedisTemplate redisTemplate;
    private final boolean enabled;

    public ExamRuntimeStateService(
            StringRedisTemplate redisTemplate,
            @Value("${app.exam.runtime-cache-enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    public void rememberStarted(Long attemptId, LocalDateTime deadlineAt, LocalDateTime now) {
        if (!enabled) {
            return;
        }
        Duration ttl = Duration.between(now, deadlineAt).plusHours(1);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofHours(1);
        }
        try {
            redisTemplate.opsForValue().set(deadlineKey(attemptId), deadlineAt.toString(), ttl);
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while caching exam deadline for attempt {}", attemptId);
        }
    }

    public void rememberSaved(Long attemptId, LocalDateTime savedAt) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    savedKey(attemptId),
                    savedAt.toString(),
                    Duration.ofHours(24)
            );
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while caching exam save state for attempt {}", attemptId);
        }
    }

    public void clear(Long attemptId) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.delete(java.util.List.of(deadlineKey(attemptId), savedKey(attemptId)));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while clearing exam state for attempt {}", attemptId);
        }
    }

    private String deadlineKey(Long attemptId) {
        return PREFIX + attemptId + ":deadline";
    }

    private String savedKey(Long attemptId) {
        return PREFIX + attemptId + ":last-saved";
    }
}
