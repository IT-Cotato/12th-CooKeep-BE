package com.cookeep.cookeep.domain.user.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

@Component
public class RedisAuthSessionStore implements AuthSessionStore {

	private static final String KEY_PREFIX = "auth:session:";
	private static final String VALUE_SEPARATOR = ":";
	private static final DefaultRedisScript<Long> CREATE_SCRIPT = new DefaultRedisScript<>("""
		redis.call('SET', KEYS[1], ARGV[1], 'PXAT', ARGV[2])
		return 1
		""", Long.class);

	private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
		local current = redis.call('GET', KEYS[1])
		if not current then
			return 0
		end

		local separator = string.find(current, ':')
		if not separator then
			return 0
		end

		local storedSessionId = string.sub(current, 1, separator - 1)
		if storedSessionId ~= ARGV[1] then
			return 2
		end

		local expected = ARGV[1] .. ':' .. ARGV[2]
		if current ~= expected then
			redis.call('DEL', KEYS[1])
			return 3
		end

		local nextValue = ARGV[1] .. ':' .. ARGV[3]
		redis.call('SET', KEYS[1], nextValue, 'PXAT', ARGV[4])
		return 1
		""", Long.class);

	private final StringRedisTemplate redisTemplate;

	public RedisAuthSessionStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void create(Long userId, String sessionId, String rawRefreshToken, Instant expiresAt) {
		assertFutureExpiration(expiresAt);
		try {
			redisTemplate.execute(
				CREATE_SCRIPT,
				List.of(key(userId)),
				value(sessionId, digest(rawRefreshToken)),
				String.valueOf(expiresAt.toEpochMilli())
			);
		} catch (RuntimeException e) {
			throw unavailable(e);
		}
	}

	@Override
	public RefreshRotationResult rotate(
		Long userId,
		String sessionId,
		String currentRawToken,
		String nextRawToken,
		Instant expiresAt
	) {
		assertFutureExpiration(expiresAt);
		try {
			Long result = redisTemplate.execute(
				ROTATE_SCRIPT,
				List.of(key(userId)),
				sessionId,
				digest(currentRawToken),
				digest(nextRawToken),
				String.valueOf(expiresAt.toEpochMilli())
			);

			if (result == null) {
				throw new IllegalStateException("Redis rotation returned no result");
			}

			return switch (result.intValue()) {
				case 1 -> RefreshRotationResult.ROTATED;
				case 2 -> RefreshRotationResult.DIFFERENT_SESSION;
				case 3 -> RefreshRotationResult.REUSE_DETECTED;
				default -> RefreshRotationResult.SESSION_NOT_FOUND;
			};
		} catch (AppException e) {
			throw e;
		} catch (RuntimeException e) {
			throw unavailable(e);
		}
	}

	@Override
	public boolean isActive(Long userId, String sessionId) {
		try {
			String storedValue = redisTemplate.opsForValue().get(key(userId));
			return storedValue != null && storedValue.startsWith(sessionId + VALUE_SEPARATOR);
		} catch (RuntimeException e) {
			throw unavailable(e);
		}
	}

	@Override
	public void revoke(Long userId) {
		try {
			redisTemplate.delete(key(userId));
		} catch (RuntimeException e) {
			throw unavailable(e);
		}
	}

	private void assertFutureExpiration(Instant expiresAt) {
		if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}

	private String key(Long userId) {
		return KEY_PREFIX + userId;
	}

	private String value(String sessionId, String digest) {
		return sessionId + VALUE_SEPARATOR + digest;
	}

	private String digest(String rawToken) {
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(sha256.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private AppException unavailable(RuntimeException cause) {
		return new AppException(ErrorCode.AUTH_SESSION_UNAVAILABLE);
	}
}
