package com.cookeep.cookeep.security;


import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.time.Instant;

@Component
public class JwtTokenProvider {

	private final Key accessKey;
	private final Key refreshKey;

	// 액세스토큰 30분
	// 리프레쉬 토큰 14일
	private static final long ACCESS_TTL  = 30 * 60 * 1000L;
	private static final long REFRESH_TTL = 14L * 24 * 60 * 60 * 1000L;

	public JwtTokenProvider(
		@Value("${jwt.access-secret}") String accessSecret,
		@Value("${jwt.refresh-secret}") String refreshSecret
	) {
		this.accessKey  = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
		this.refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
	}

	// 액세스 토큰 생성
	public String createAccessToken(Long userId) {
		Date now = new Date(); // 발급 시간
		Date exp = new Date(now.getTime() + ACCESS_TTL); // 만료 시간 (발급 시간 + 기한)

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE) // 명시적으로 typ=JWT임을 넣어줌
			.setSubject(String.valueOf(userId)) // userId를 기준으로 액세스토큰 생성
			.setIssuedAt(now) // 발급 시간
			.setExpiration(exp) // 만료 시간
			.claim("typ", "access")
			.claim("jti", generateJti())
			.signWith(accessKey, SignatureAlgorithm.HS256)
			.compact();
	}

	// 리프레쉬 토큰 발급
	public String createRefreshToken(Long userId) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + REFRESH_TTL);

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(String.valueOf(userId))
			.setIssuedAt(now)
			.setExpiration(exp)
			.claim("typ", "refresh")
			.claim("jti", generateJti())
			.signWith(refreshKey, SignatureAlgorithm.HS256)
			.compact();
	}

	public String createAccessToken(Long userId, String sessionId) {
		Date now = new Date();
		Date exp = new Date(now.getTime() + ACCESS_TTL);

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(String.valueOf(userId))
			.setIssuedAt(now)
			.setExpiration(exp)
			.claim("typ", "access")
			.claim("sid", sessionId)
			.claim("jti", generateJti())
			.signWith(accessKey, SignatureAlgorithm.HS256)
			.compact();
	}

	public String createRefreshToken(Long userId, String sessionId, Instant expiresAt) {
		Date now = new Date();

		return Jwts.builder()
			.setHeaderParam(Header.TYPE, Header.JWT_TYPE)
			.setSubject(String.valueOf(userId))
			.setIssuedAt(now)
			.setExpiration(Date.from(expiresAt))
			.claim("typ", "refresh")
			.claim("sid", sessionId)
			.claim("jti", generateJti())
			.signWith(refreshKey, SignatureAlgorithm.HS256)
			.compact();
	}

	public TokenClaims parseAccessToken(String token) {
		return parseToken(token, accessKey, "access");
	}

	public TokenClaims parseRefreshToken(String token) {
		return parseToken(token, refreshKey, "refresh");
	}

	// 토큰 유효성 검사
	public boolean validateToken(String token, boolean refresh) {
		try {
			Jwts.parserBuilder()
				.setSigningKey(refresh ? refreshKey : accessKey)
				.setAllowedClockSkewSeconds(60)
				.build()
				.parseClaimsJws(token);
			return true;
		} catch (ExpiredJwtException e) {
			return false;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}

	// 토큰 내 userId 추출
	public Long getUserId(String token, boolean refresh) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(refresh ? refreshKey : accessKey)
			.setAllowedClockSkewSeconds(60)
			.build()
			.parseClaimsJws(token)
			.getBody();
		return Long.valueOf(claims.getSubject());
	}

	private String generateJti() {
		return java.util.UUID.randomUUID().toString();
	}
	private TokenClaims parseToken(String token, Key signingKey, String expectedType) {
		Claims claims = Jwts.parserBuilder()
			.setSigningKey(signingKey)
			.setAllowedClockSkewSeconds(60)
			.build()
			.parseClaimsJws(token)
			.getBody();

		String tokenType = claims.get("typ", String.class);
		String sessionId = claims.get("sid", String.class);
		if (!expectedType.equals(tokenType) || sessionId == null || sessionId.isBlank()) {
			throw new JwtException("Invalid token type or missing session id");
		}

		return new TokenClaims(
			Long.valueOf(claims.getSubject()),
			sessionId,
			claims.getExpiration().toInstant()
		);
	}
}
