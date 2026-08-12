package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;

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
import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;

@ExtendWith(MockitoExtension.class)
class RedisReauthenticationStoreFailureTest {

	@Mock
	private StringRedisTemplate redisTemplate;
	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisReauthenticationStore store;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		store = new RedisReauthenticationStore(redisTemplate);
	}

	@Test
	void issueFailsClosedWhenRedisIsUnavailable() {
		doThrow(new RedisConnectionFailureException("redis unavailable"))
			.when(valueOperations)
			.set(anyString(), anyString(), any(Duration.class));

		assertUnavailable(() -> store.issue(
			1L,
			ReauthenticationPurpose.CHANGE_PASSWORD
		));
	}

	@Test
	void consumeFailsClosedWhenRedisIsUnavailable() {
		when(valueOperations.getAndDelete(anyString()))
			.thenThrow(new RedisConnectionFailureException("redis unavailable"));

		assertUnavailable(() -> store.consume(
			1L,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			"reauth-token"
		));
	}

	private void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action)
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.REAUTHENTICATION_UNAVAILABLE);
	}
}
