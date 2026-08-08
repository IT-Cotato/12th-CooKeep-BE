package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import org.springframework.data.redis.core.script.RedisScript;
@ExtendWith(MockitoExtension.class)
class RedisAuthSessionStoreFailureTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisAuthSessionStore store;

	@BeforeEach
	void setUp() {
		store = new RedisAuthSessionStore(redisTemplate);
	}

	@Test
	void createFailsClosedWhenRedisIsUnavailable() {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
			.thenThrow(new RedisConnectionFailureException("redis unavailable"));

		assertUnavailable(() ->
			store.create(
				1L,
				"session-id",
				"refresh-token",
				Instant.now().plus(Duration.ofMinutes(5))
			)
		);
	}

	@Test
	void rotateFailsClosedWhenRedisIsUnavailable() {
		when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any()))
			.thenThrow(new RedisConnectionFailureException("redis unavailable"));

		assertUnavailable(() -> store.rotate(
			1L,
			"session-id",
			"refresh-token",
			"next-refresh-token"
		));
	}

	@Test
	void activeCheckFailsClosedWhenRedisIsUnavailable() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get(anyString()))
			.thenThrow(new RedisConnectionFailureException("redis unavailable"));

		assertUnavailable(() -> store.isActive(1L, "session-id"));
	}

	private void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action)
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.AUTH_SESSION_UNAVAILABLE);
	}
}
