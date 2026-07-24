package com.learningplatform.auth.security;

import com.learningplatform.common.api.ErrorCode;
import com.learningplatform.common.config.LoginProtectionProperties;
import com.learningplatform.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginProtectionServiceTests {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void disabledProtectionDoesNotUseRedis() {
        LoginProtectionService service = service(false);

        service.checkAllowed("alice", "127.0.0.1");
        service.recordFailure("alice", "127.0.0.1");
        service.recordSuccess("alice");

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void rejectsBlockedLoginBeforePasswordCheck() {
        LoginProtectionService service = service(true);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.checkAllowed("alice", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    void blocksAccountWhenFailureThresholdIsReached() {
        LoginProtectionService service = service(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(5L, 1L);

        assertThatThrownBy(() -> service.recordFailure("alice", "127.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("登录失败次数过多，请稍后重试");

        verify(valueOperations).set(anyString(), eq("1"), eq(Duration.ofMinutes(15)));
        verify(redisTemplate).expire(anyString(), eq(Duration.ofMinutes(15)));
    }

    @Test
    void clearsOnlyAccountStateAfterSuccessfulLogin() {
        LoginProtectionService service = service(true);

        service.recordSuccess("alice");

        verify(redisTemplate).delete(anyCollection());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void failsOpenWhenRedisIsUnavailable() {
        LoginProtectionService service = service(true);
        when(redisTemplate.hasKey(anyString()))
                .thenThrow(new RedisConnectionFailureException("redis unavailable"));

        assertThatCode(() -> service.checkAllowed("alice", "127.0.0.1"))
                .doesNotThrowAnyException();
    }

    private LoginProtectionService service(boolean enabled) {
        return new LoginProtectionService(
                redisTemplate,
                new LoginProtectionProperties(enabled, 5, 20, Duration.ofMinutes(15))
        );
    }
}
