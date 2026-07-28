/* 文件职责：实现考试运行态State业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 * 所属模块：试卷、考试、作答、阅卷、统计与错题；所在分层：业务服务层。
 * 维护提示：修改本文件时应同步检查相关 DTO、Mapper、Service、Controller 与测试。
 */
package com.learningplatform.exam.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
/**
 * 实现考试运行态State业务规则，协调持久化组件并维护事务、权限、状态与幂等边界。
 *
 * <p>职责边界：业务状态变化在此集中完成；跨表写入需保持事务一致性。</p>
 */
public class ExamRuntimeStateService {
    private static final Logger log = LoggerFactory.getLogger(ExamRuntimeStateService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    /** 定义 PREFIX 常量，统一该组件使用的固定规则或默认值。 */
    private static final String PREFIX = "exam:attempt:";
    static final String DEADLINE_INDEX_KEY = "exam:attempt:deadlines";
    private static final DefaultRedisScript<List> CLAIM_EXPIRED_SCRIPT = new DefaultRedisScript<>(
            """
            local attemptIds = redis.call(
                'ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1],
                'LIMIT', 0, ARGV[2]
            )
            if #attemptIds > 0 then
                redis.call('ZREM', KEYS[1], unpack(attemptIds))
            end
            return attemptIds
            """,
            List.class
    );

    /** 保存redis模板，供该类型的业务逻辑读取或更新。 */
    private final StringRedisTemplate redisTemplate;
    /** 保存启用状态，供该类型的业务逻辑读取或更新。 */
    private final boolean enabled;

    /** 注入并保存该组件运行所需依赖，不在构造阶段执行业务操作。 */
    public ExamRuntimeStateService(
            StringRedisTemplate redisTemplate,
            @Value("${app.exam.runtime-cache-enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.enabled = enabled;
    }

    /** 执行 rememberStarted 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    public void rememberStarted(Long attemptId, LocalDateTime deadlineAt, LocalDateTime now) {
        if (!enabled) {
            return;
        }
        Duration ttl = Duration.between(now, deadlineAt).plusHours(1);
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofHours(1);
        }
        try {
            redisTemplate.opsForZSet().add(
                    DEADLINE_INDEX_KEY,
                    attemptId.toString(),
                    epochMillis(deadlineAt)
            );
            redisTemplate.opsForValue().set(deadlineKey(attemptId), deadlineAt.toString(), ttl);
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while indexing exam deadline for attempt {}", attemptId);
        }
    }

    /**
     * Atomically removes and returns due attempts. Removing in the same Lua script prevents
     * multiple application instances from claiming the same Redis entry.
     */
    public ExpiredAttemptClaim claimExpired(LocalDateTime now, int limit) {
        if (!enabled) {
            return ExpiredAttemptClaim.redisUnavailable();
        }
        try {
            List<?> rawAttemptIds = redisTemplate.execute(
                    CLAIM_EXPIRED_SCRIPT,
                    List.of(DEADLINE_INDEX_KEY),
                    Long.toString(epochMillis(now)),
                    Integer.toString(limit)
            );
            if (rawAttemptIds == null || rawAttemptIds.isEmpty()) {
                return ExpiredAttemptClaim.available(List.of());
            }
            List<Long> attemptIds = new ArrayList<>(rawAttemptIds.size());
            for (Object rawAttemptId : rawAttemptIds) {
                attemptIds.add(Long.valueOf(rawAttemptId.toString()));
            }
            return ExpiredAttemptClaim.available(List.copyOf(attemptIds));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while claiming expired exam attempts");
            return ExpiredAttemptClaim.redisUnavailable();
        }
    }

    /**
     * Returns a failed claim to the deadline index with a short retry delay.
     * MySQL remains the source of truth and the periodic database scan is the final fallback.
     */
    public void requeueExpired(Long attemptId, LocalDateTime retryAt) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForZSet().add(
                    DEADLINE_INDEX_KEY,
                    attemptId.toString(),
                    epochMillis(retryAt)
            );
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while requeueing expired exam attempt {}", attemptId);
        }
    }

    /** 执行 rememberSaved 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
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

    /** 删除、移除或清理，同时维护关联数据和权限不变量。 */
    public void clear(Long attemptId) {
        if (!enabled) {
            return;
        }
        try {
            redisTemplate.opsForZSet().remove(DEADLINE_INDEX_KEY, attemptId.toString());
            redisTemplate.delete(java.util.List.of(deadlineKey(attemptId), savedKey(attemptId)));
        } catch (DataAccessException exception) {
            log.warn("Redis unavailable while clearing exam state for attempt {}", attemptId);
        }
    }

    private long epochMillis(LocalDateTime value) {
        return value.atZone(BUSINESS_ZONE).toInstant().toEpochMilli();
    }

    public record ExpiredAttemptClaim(List<Long> attemptIds, boolean redisAvailable) {
        private static ExpiredAttemptClaim available(List<Long> attemptIds) {
            return new ExpiredAttemptClaim(attemptIds, true);
        }

        private static ExpiredAttemptClaim redisUnavailable() {
            return new ExpiredAttemptClaim(List.of(), false);
        }
    }

    /** 执行 deadlineKey 对应的领域用例，并在服务层维护权限、事务和状态约束。 */
    private String deadlineKey(Long attemptId) {
        return PREFIX + attemptId + ":deadline";
    }

    /** 更新d键，通过返回值或版本条件识别并发状态变化。 */
    private String savedKey(Long attemptId) {
        return PREFIX + attemptId + ":last-saved";
    }
}
