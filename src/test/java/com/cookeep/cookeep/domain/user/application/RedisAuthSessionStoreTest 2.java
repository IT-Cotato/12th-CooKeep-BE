package com.cookeep.cookeep.domain.user.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;

class RedisAuthSessionStoreTest {

	private static final long USER_ID = 9_910_001L;
	private static final String KEY = "auth:session:" + USER_ID;
	private static final DefaultRedisScript<Long> EXPIRATION_TIME_SCRIPT = new DefaultRedisScript<>(
		"return redis.call('PEXPIRETIME', KEYS[1])", Long.class
	);

	private static LettuceConnectionFactory connectionFactory;
	private static StringRedisTemplate redisTemplate;
	private static RedisAuthSessionStore store;

	@BeforeAll
	static void setUpRedis() {
		RedisStandaloneConfiguration configuration =
			new RedisStandaloneConfiguration("localhost", 6379);
		connectionFactory = new LettuceConnectionFactory(configuration);
		connectionFactory.afterPropertiesSet();
		connectionFactory.start();

		redisTemplate = new StringRedisTemplate(connectionFactory);
		redisTemplate.afterPropertiesSet();
		store = new RedisAuthSessionStore(redisTemplate);

		assertThat(connectionFactory.getConnection().ping()).isEqualTo("PONG");
	}

	@AfterEach
	void cleanTestKeys() {
		Set<String> keys = redisTemplate.keys("auth:session:991000*");
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
	void createStoresDigestOnlyWithTtl() {
		store.create(
			USER_ID,
			"session-id",
			"raw-refresh-token",
			Instant.now().plus(Duration.ofMinutes(5))
		);

		String value = redisTemplate.opsForValue().get(KEY);
		assertThat(value)
			.startsWith("session-id:")
			.doesNotContain("raw-refresh-token");
		assertThat(redisTemplate.getExpire(KEY, TimeUnit.SECONDS)).isBetween(295L, 300L);
	}

	@Test
	void rotateReplacesDigestAndKeepsExistingAbsoluteExpiration() {
		store.create(
			USER_ID,
			"session-id",
			"current-token",
			Instant.now().plus(Duration.ofMinutes(5))
		);

		Long expiresAtBefore = redisTemplate.execute(
			EXPIRATION_TIME_SCRIPT,
			List.of(KEY)
		);
		RefreshRotationResult result = store.rotate(
			USER_ID,
			"session-id",
			"current-token",
			"next-token"
		);
		Long expiresAtAfter = redisTemplate.execute(
			EXPIRATION_TIME_SCRIPT,
			List.of(KEY)
		);

		assertThat(result).isEqualTo(RefreshRotationResult.ROTATED);
		assertThat(redisTemplate.opsForValue().get(KEY))
			.startsWith("session-id:")
			.doesNotContain("current-token")
			.doesNotContain("next-token");
		assertThat(expiresAtAfter).isEqualTo(expiresAtBefore);
	}

	@Test
	void reusedTokenRevokesCurrentSession() {
		store.create(
			USER_ID,
			"session-id",
			"current-token",
			Instant.now().plus(Duration.ofMinutes(5))
		);
		store.rotate(
			USER_ID,
			"session-id",
			"current-token",
			"next-token"
		);

		RefreshRotationResult result = store.rotate(
			USER_ID,
			"session-id",
			"current-token",
			"another-token"
		);

		assertThat(result).isEqualTo(RefreshRotationResult.REUSE_DETECTED);
		assertThat(redisTemplate.hasKey(KEY)).isFalse();
	}

	@Test
	void tokenFromDifferentLoginDoesNotRevokeCurrentSession() {
		store.create(
			USER_ID,
			"current-session",
			"current-token",
			Instant.now().plus(Duration.ofMinutes(5))
		);

		RefreshRotationResult result = store.rotate(
			USER_ID,
			"old-session",
			"old-token",
			"next-token"
		);

		assertThat(result).isEqualTo(RefreshRotationResult.DIFFERENT_SESSION);
		assertThat(redisTemplate.opsForValue().get(KEY)).startsWith("current-session:");
	}

	@Test
	void concurrentRotationAllowsOneSuccessAndDetectsOneReuse() throws Exception {
		store.create(
			USER_ID,
			"session-id",
			"current-token",
			Instant.now().plus(Duration.ofMinutes(5))
		);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);

		Callable<RefreshRotationResult> rotate = () -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			return store.rotate(
				USER_ID,
				"session-id",
				"current-token",
				Thread.currentThread().getName()
			);
		};

		try {
			List<Future<RefreshRotationResult>> futures = List.of(
				executor.submit(rotate),
				executor.submit(rotate)
			);
			assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			List<RefreshRotationResult> results = List.of(
				futures.get(0).get(5, TimeUnit.SECONDS),
				futures.get(1).get(5, TimeUnit.SECONDS)
			);
			assertThat(results).containsExactlyInAnyOrder(
				RefreshRotationResult.ROTATED,
				RefreshRotationResult.REUSE_DETECTED
			);
			assertThat(redisTemplate.hasKey(KEY)).isFalse();
		} finally {
			start.countDown();
			executor.shutdownNow();
		}
	}
}
