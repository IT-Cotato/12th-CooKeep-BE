package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;

class RedisReauthenticationStoreTest {

	private static final long USER_ID = 9_900_001L;
	private static final long OTHER_USER_ID = 9_900_002L;
	private static final String TEST_KEY_PATTERN = RedisReauthenticationStore.KEY_PREFIX + "990000*";

	private static LettuceConnectionFactory connectionFactory;
	private static StringRedisTemplate redisTemplate;
	private static RedisReauthenticationStore store;

	@BeforeAll
	static void setUpRedis() {
		RedisStandaloneConfiguration configuration =
			new RedisStandaloneConfiguration("localhost", 6379);
		connectionFactory = new LettuceConnectionFactory(configuration);
		connectionFactory.afterPropertiesSet();
		connectionFactory.start();

		redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		store = new RedisReauthenticationStore(redisTemplate);

		assertThat(connectionFactory.getConnection().ping()).isEqualTo("PONG");
	}

	@AfterEach
	void cleanTestKeys() {
		Set<String> keys = redisTemplate.keys(TEST_KEY_PATTERN);
		if (keys != null && !keys.isEmpty()) {
			redisTemplate.delete(keys);
		}
	}

	@AfterAll
	static void tearDownRedis() {
		if (connectionFactory != null) {
			connectionFactory.destroy();
		}
	}

	@Test
	void issueStoresOnlyDigestWithFiveMinuteTtl() {
		String rawToken = store.issue(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD);
		String key = RedisReauthenticationStore.buildKey(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		);

		assertThat(Base64.getUrlDecoder().decode(rawToken)).hasSize(32);
		assertThat(key)
			.startsWith("auth:reauth:" + USER_ID + ":CHANGE_PASSWORD:")
			.doesNotContain(rawToken);
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("1");
		assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS)).isBetween(295L, 300L);
	}

	@Test
	void invalidTokenIsRejected() {
		assertInvalidToken(() -> store.consume(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			"forged-token"
		));
	}

	@Test
	void expiredTokenIsRejected() throws InterruptedException {
		RedisReauthenticationStore shortLivedStore = new RedisReauthenticationStore(
			redisTemplate,
			new SecureRandom(),
			Duration.ofMillis(100)
		);
		String rawToken = shortLivedStore.issue(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD
		);

		Thread.sleep(250);

		assertInvalidToken(() -> shortLivedStore.consume(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		));
	}

	@Test
	void tokenBoundToDifferentUserIsRejected() {
		String rawToken = store.issue(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD);

		assertInvalidToken(() -> store.consume(
			OTHER_USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		));

		store.consume(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD, rawToken);
	}

	@Test
	void tokenStoredForDifferentPurposeIsRejected() {
		String rawToken = store.issue(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD);
		String changePasswordKey = RedisReauthenticationStore.buildKey(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		);
		String otherPurposeKey = changePasswordKey.replace(
			":CHANGE_PASSWORD:",
			":OTHER_PURPOSE:"
		);
		redisTemplate.delete(changePasswordKey);
		redisTemplate.opsForValue().set(
			otherPurposeKey,
			"1",
			ReauthenticationPolicy.TOKEN_TTL
		);

		assertInvalidToken(() -> store.consume(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		));
		assertThat(redisTemplate.opsForValue().get(otherPurposeKey)).isEqualTo("1");
	}

	@Test
	void consumedTokenCannotBeReused() {
		String rawToken = store.issue(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD);

		store.consume(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD, rawToken);

		assertInvalidToken(() -> store.consume(
			USER_ID,
			ReauthenticationPurpose.CHANGE_PASSWORD,
			rawToken
		));
	}

	@Test
	void concurrentConsumptionAllowsExactlyOneSuccess() throws Exception {
		String rawToken = store.issue(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Callable<Boolean> consume = () -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			try {
				store.consume(USER_ID, ReauthenticationPurpose.CHANGE_PASSWORD, rawToken);
				return true;
			} catch (AppException e) {
				if (e.getErrorCode() == ErrorCode.INVALID_REAUTH_TOKEN) {
					return false;
				}
				throw e;
			}
		};

		try {
			List<Future<Boolean>> futures = List.of(
				executor.submit(consume),
				executor.submit(consume)
			);
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			long successCount = 0;
			for (Future<Boolean> future : futures) {
				if (future.get(5, TimeUnit.SECONDS)) {
					successCount++;
				}
			}
			assertThat(successCount).isEqualTo(1);
		} finally {
			start.countDown();
			executor.shutdownNow();
		}
	}

	private void assertInvalidToken(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action)
			.isInstanceOf(AppException.class)
			.extracting("errorCode")
			.isEqualTo(ErrorCode.INVALID_REAUTH_TOKEN);
	}
}
