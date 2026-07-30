package com.cookeep.cookeep.domain.user.application;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.entity.ReauthenticationPurpose;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RedisReauthenticationStore implements ReauthenticationStore {

	static final String KEY_PREFIX = "auth:reauth:";
	private static final String STORED_VALUE = "1";
	private static final int TOKEN_BYTES = 32;

	private final StringRedisTemplate redisTemplate;
	private final SecureRandom secureRandom;
	private final Duration tokenTtl;

	@Autowired
	public RedisReauthenticationStore(StringRedisTemplate redisTemplate) {
		this(redisTemplate, new SecureRandom(), ReauthenticationPolicy.TOKEN_TTL);
	}

	RedisReauthenticationStore(
		StringRedisTemplate redisTemplate,
		SecureRandom secureRandom,
		Duration tokenTtl
	) {
		this.redisTemplate = redisTemplate;
		this.secureRandom = secureRandom;
		this.tokenTtl = tokenTtl;
	}

	@Override
	public String issue(Long userId, ReauthenticationPurpose purpose) {
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		String key = buildKey(userId, purpose, rawToken);

		try {
			redisTemplate.opsForValue().set(key, STORED_VALUE, tokenTtl);
			return rawToken;
		} catch (DataAccessException e) {
			log.error("Failed to issue reauthentication token. userId={}, purpose={}", userId, purpose, e);
			throw new AppException(ErrorCode.REAUTHENTICATION_UNAVAILABLE);
		}
	}

	@Override
	public void consume(Long userId, ReauthenticationPurpose purpose, String rawToken) {
		String key = buildKey(userId, purpose, rawToken);

		try {
			String value = redisTemplate.opsForValue().getAndDelete(key);
			if (value == null) {
				throw new AppException(ErrorCode.INVALID_REAUTH_TOKEN);
			}
		} catch (DataAccessException e) {
			log.error("Failed to consume reauthentication token. userId={}, purpose={}", userId, purpose, e);
			throw new AppException(ErrorCode.REAUTHENTICATION_UNAVAILABLE);
		}
	}

	static String buildKey(Long userId, ReauthenticationPurpose purpose, String rawToken) {
		return KEY_PREFIX + userId + ":" + purpose.name() + ":" + digest(rawToken);
	}

	private static String digest(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}
}
