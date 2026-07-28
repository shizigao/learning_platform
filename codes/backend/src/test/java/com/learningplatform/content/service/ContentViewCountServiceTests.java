package com.learningplatform.content.service;

import com.learningplatform.classroom.mapper.ClassScopeMapper;
import com.learningplatform.classroom.service.ClassroomService;
import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.exception.BusinessException;
import com.learningplatform.content.domain.ContentDistributionMode;
import com.learningplatform.content.domain.ContentStatus;
import com.learningplatform.content.domain.LearningContent;
import com.learningplatform.content.mapper.ContentFileMapper;
import com.learningplatform.content.mapper.LearningContentMapper;
import com.learningplatform.content.storage.MinioStorageService;
import com.learningplatform.user.service.UserAvatarService;
import com.learningplatform.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContentViewCountServiceTests {

    @Test
    void buffersIncrementAndReturnsDatabasePlusPending() {
        Fixture fixture = new Fixture(true);
        when(fixture.hashOperations.increment(
                ContentViewCountService.PENDING_KEY,
                "12",
                1L
        )).thenReturn(3L);

        assertThat(fixture.service.incrementAndGet(12L, 10L)).isEqualTo(13L);
        verify(fixture.mapper, never()).incrementViewCountBy(any(), any(Long.class));
    }

    @Test
    void fallsBackToDatabaseWhenRedisIncrementFails() {
        Fixture fixture = new Fixture(true);
        when(fixture.hashOperations.increment(
                ContentViewCountService.PENDING_KEY,
                "12",
                1L
        )).thenThrow(new IllegalStateException("redis unavailable"));
        when(fixture.mapper.incrementViewCountBy(12L, 1L)).thenReturn(1);

        assertThat(fixture.service.incrementAndGet(12L, 10L)).isEqualTo(11L);
        verify(fixture.mapper).incrementViewCountBy(12L, 1L);
    }

    @Test
    void detailCountIncludesPendingDelta() {
        Fixture fixture = new Fixture(true);
        when(fixture.hashOperations.get(ContentViewCountService.PENDING_KEY, "15"))
                .thenReturn("4");

        assertThat(fixture.service.currentCount(15L, 20L)).isEqualTo(24L);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void flushesAtomicDrainUnderDistributedLock() {
        Fixture fixture = new Fixture(true);
        when(fixture.valueOperations.setIfAbsent(
                eq(ContentViewCountService.FLUSH_LOCK_KEY),
                any(String.class),
                eq(Duration.ofMinutes(2))
        )).thenReturn(true);
        when(fixture.redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(ContentViewCountService.PENDING_KEY))
        )).thenReturn(List.of("12", "3", "15", "2"));
        when(fixture.mapper.incrementViewCountBy(12L, 3L)).thenReturn(1);
        when(fixture.mapper.incrementViewCountBy(15L, 2L)).thenReturn(1);

        fixture.service.flushPendingViewCounts();

        verify(fixture.mapper).incrementViewCountBy(12L, 3L);
        verify(fixture.mapper).incrementViewCountBy(15L, 2L);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void failedDatabaseDeltaIsReturnedToPendingHash() {
        Fixture fixture = new Fixture(true);
        when(fixture.valueOperations.setIfAbsent(
                eq(ContentViewCountService.FLUSH_LOCK_KEY),
                any(String.class),
                eq(Duration.ofMinutes(2))
        )).thenReturn(true);
        when(fixture.redisTemplate.execute(
                any(RedisScript.class),
                eq(List.of(ContentViewCountService.PENDING_KEY))
        )).thenReturn(List.of("12", "3"));
        when(fixture.mapper.incrementViewCountBy(12L, 3L))
                .thenThrow(new IllegalStateException("database unavailable"));
        when(fixture.hashOperations.increment(
                ContentViewCountService.PENDING_KEY,
                "12",
                3L
        )).thenReturn(3L);

        fixture.service.flushPendingViewCounts();

        verify(fixture.hashOperations).increment(
                ContentViewCountService.PENDING_KEY,
                "12",
                3L
        );
    }

    @Test
    void rejectedClassAccessIsNotCounted() {
        LearningContentMapper mapper = mock(LearningContentMapper.class);
        ContentAccessService accessService = mock(ContentAccessService.class);
        ContentViewCountService viewCountService = mock(ContentViewCountService.class);
        LearningContent content = new LearningContent();
        content.setId(18L);
        content.setPublisherId(3L);
        content.setStatus(ContentStatus.PUBLISHED);
        content.setDistributionMode(ContentDistributionMode.CLASS);
        content.setViewCount(9L);
        when(mapper.findById(18L)).thenReturn(Optional.of(content));
        when(accessService.hasAccess(7L, false, content)).thenReturn(false);

        LearningContentService contentService = new LearningContentService(
                mapper,
                mock(ContentFileMapper.class),
                mock(ContentCategoryService.class),
                accessService,
                mock(MinioStorageService.class),
                mock(ClassScopeMapper.class),
                mock(ClassroomService.class),
                mock(UserService.class),
                mock(UserAvatarService.class),
                viewCountService
        );

        assertThatThrownBy(() -> contentService.publishedDetail(18L, 7L, false))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(viewCountService);
    }

    private static final class Fixture {
        private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        private final LearningContentMapper mapper = mock(LearningContentMapper.class);
        @SuppressWarnings("unchecked")
        private final HashOperations<String, Object, Object> hashOperations =
                mock(HashOperations.class);
        @SuppressWarnings("unchecked")
        private final ValueOperations<String, String> valueOperations =
                mock(ValueOperations.class);
        private final ContentViewCountService service;

        private Fixture(boolean enabled) {
            when(redisTemplate.opsForHash()).thenReturn(hashOperations);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            service = new ContentViewCountService(redisTemplate, mapper, enabled);
        }
    }
}
