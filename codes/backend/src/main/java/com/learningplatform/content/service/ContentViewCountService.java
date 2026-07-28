/*
 * 文件职责：使用 Redis 缓冲学习资料浏览增量，并定时可靠回写 MySQL。
 * 所属模块：学习资料；所在分层：业务服务层。
 */
package com.learningplatform.content.service;

import com.learningplatform.content.mapper.LearningContentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ContentViewCountService {
    private static final Logger log = LoggerFactory.getLogger(ContentViewCountService.class);

    static final String PENDING_KEY = "content:view-count:pending";
    static final String FLUSH_LOCK_KEY = "content:view-count:flush-lock";
    private static final Duration FLUSH_LOCK_TTL = Duration.ofMinutes(2);

    /*
     * HGETALL 与 DEL 必须在同一个 Redis 命令中完成。新请求在 DEL 后会写入一个
     * 新 hash，因此不会与本批次混在一起，也不会被多实例重复回写。
     */
    private static final DefaultRedisScript<List> DRAIN_SCRIPT = new DefaultRedisScript<>(
            """
            local entries = redis.call('HGETALL', KEYS[1])
            if #entries > 0 then
                redis.call('DEL', KEYS[1])
            end
            return entries
            """,
            List.class
    );

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final LearningContentMapper contentMapper;
    private final boolean enabled;

    public ContentViewCountService(
            StringRedisTemplate redisTemplate,
            LearningContentMapper contentMapper,
            @Value("${app.redis.content-view-buffer-enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.contentMapper = contentMapper;
        this.enabled = enabled;
    }

    /**
     * 记录一次已通过状态及访问范围校验的浏览，并返回本次响应应展示的浏览量。
     * Redis 不可用时退化为同步更新 MySQL，避免丢失浏览记录。
     */
    public long incrementAndGet(Long contentId, long databaseCount) {
        if (!enabled) {
            contentMapper.incrementViewCountBy(contentId, 1L);
            return databaseCount + 1L;
        }
        try {
            Long pending = redisTemplate.opsForHash().increment(
                    PENDING_KEY,
                    contentId.toString(),
                    1L
            );
            return databaseCount + (pending == null ? 1L : pending);
        } catch (RuntimeException exception) {
            log.warn(
                    "Redis unavailable while buffering content view; falling back to MySQL, contentId={}",
                    contentId,
                    exception
            );
            contentMapper.incrementViewCountBy(contentId, 1L);
            return databaseCount + 1L;
        }
    }

    /** 将数据库值与尚未回写的 Redis 增量合并，用于详情响应。 */
    public long currentCount(Long contentId, long databaseCount) {
        if (!enabled) {
            return databaseCount;
        }
        try {
            Object pendingValue = redisTemplate.opsForHash().get(
                    PENDING_KEY,
                    contentId.toString()
            );
            return databaseCount + parsePositiveLong(pendingValue);
        } catch (RuntimeException exception) {
            log.warn(
                    "Redis unavailable while reading pending content views, contentId={}",
                    contentId
            );
            return databaseCount;
        }
    }

    /**
     * 多实例共享同一把短期锁；拿到锁的实例原子取走当前批次，再逐项写回。
     * 单项数据库写入失败时，仅将该项重新放回 pending，不重复已成功的项。
     */
    @Scheduled(
            fixedDelayString = "${app.redis.content-view-flush-ms:30000}",
            initialDelayString = "${app.redis.content-view-flush-initial-delay-ms:30000}"
    )
    public void flushPendingViewCounts() {
        if (!enabled) {
            return;
        }
        String lockToken = UUID.randomUUID().toString();
        boolean locked;
        try {
            locked = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(
                    FLUSH_LOCK_KEY,
                    lockToken,
                    FLUSH_LOCK_TTL
            ));
        } catch (RuntimeException exception) {
            log.warn("Redis unavailable while acquiring content view flush lock");
            return;
        }
        if (!locked) {
            return;
        }

        try {
            List<?> drained = redisTemplate.execute(
                    DRAIN_SCRIPT,
                    Collections.singletonList(PENDING_KEY)
            );
            List<ViewDelta> deltas = parseDrainedEntries(drained);
            if (deltas.isEmpty()) {
                return;
            }

            long flushed = 0L;
            for (ViewDelta delta : deltas) {
                try {
                    int rows = contentMapper.incrementViewCountBy(
                            delta.contentId(),
                            delta.delta()
                    );
                    if (rows == 1) {
                        flushed += delta.delta();
                    } else {
                        log.info(
                                "Discarding view delta for unavailable content, contentId={} delta={}",
                                delta.contentId(),
                                delta.delta()
                        );
                    }
                } catch (RuntimeException exception) {
                    restore(delta);
                    log.warn(
                            "Failed to flush content view delta; returned to pending, contentId={} delta={}",
                            delta.contentId(),
                            delta.delta(),
                            exception
                    );
                }
            }
            log.info(
                    "Content view count flush completed, contents={} views={}",
                    deltas.size(),
                    flushed
            );
        } catch (RuntimeException exception) {
            /*
             * Redis 脚本具有原子性：脚本失败时不会留下“只读取、未删除”的半完成状态。
             * 成功 drain 后的数据库单项失败由 restore 单独补回。
             */
            log.warn("Unable to drain pending content view counts", exception);
        } finally {
            try {
                redisTemplate.execute(
                        UNLOCK_SCRIPT,
                        Collections.singletonList(FLUSH_LOCK_KEY),
                        lockToken
                );
            } catch (RuntimeException exception) {
                log.warn("Redis unavailable while releasing content view flush lock");
            }
        }
    }

    private void restore(ViewDelta delta) {
        redisTemplate.opsForHash().increment(
                PENDING_KEY,
                delta.contentId().toString(),
                delta.delta()
        );
    }

    private List<ViewDelta> parseDrainedEntries(List<?> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<ViewDelta> result = new ArrayList<>(entries.size() / 2);
        for (int index = 0; index + 1 < entries.size(); index += 2) {
            try {
                long contentId = Long.parseLong(String.valueOf(entries.get(index)));
                long delta = Long.parseLong(String.valueOf(entries.get(index + 1)));
                if (contentId > 0L && delta > 0L) {
                    result.add(new ViewDelta(contentId, delta));
                }
            } catch (NumberFormatException exception) {
                log.warn(
                        "Ignoring invalid content view counter entry, field={} value={}",
                        entries.get(index),
                        entries.get(index + 1)
                );
            }
        }
        return result;
    }

    private long parsePositiveLong(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(String.valueOf(value)));
        } catch (NumberFormatException exception) {
            log.warn("Ignoring invalid pending content view count: {}", value);
            return 0L;
        }
    }

    private record ViewDelta(Long contentId, long delta) {
    }
}
