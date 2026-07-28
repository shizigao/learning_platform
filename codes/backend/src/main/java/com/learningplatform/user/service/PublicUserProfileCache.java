package com.learningplatform.user.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.learningplatform.common.redis.RedisJsonCache;
import com.learningplatform.user.dto.PublicUserProfileResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 缓存公开个人中心聚合结果。该结果需要联合用户、角色、头像和资料统计，多次访问时
 * 由 Redis 承担短期读取；所有相关写路径均应调用失效方法。
 */
@Component
public class PublicUserProfileCache {
    private static final TypeReference<PublicUserProfileResponse> PROFILE_TYPE =
            new TypeReference<>() {
            };

    private final RedisJsonCache redisJsonCache;

    public PublicUserProfileCache(RedisJsonCache redisJsonCache) {
        this.redisJsonCache = redisJsonCache;
    }

    public PublicUserProfileResponse get(
            Long userId,
            Supplier<PublicUserProfileResponse> loader
    ) {
        return redisJsonCache.get(
                key(userId),
                PROFILE_TYPE,
                Duration.ofMinutes(2),
                loader
        );
    }

    public void evictAfterCommit(Long userId) {
        if (userId != null) {
            redisJsonCache.evictAfterCommit(key(userId));
        }
    }

    private String key(Long userId) {
        return "lp:v1:user:public-profile:" + userId;
    }
}
