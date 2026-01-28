package com.cookeep.cookeep.domain.user.application;

import static com.cookeep.cookeep.domain.user.entity.Provider.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.response.KakaoLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.config.JwtTokenProvider;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dao.UserSessionRepository;
import com.cookeep.cookeep.domain.user.dto.KakaoUserInfoResponseDTO;
import com.cookeep.cookeep.domain.user.entity.NextStep;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserAuth;
import com.cookeep.cookeep.domain.user.entity.UserSession;
import com.cookeep.cookeep.domain.user.entity.UserStatus;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

	private final KakaoOAuthProvider kakaoOAuthProvider;
	private final UserRepository userRepository;
	private final UserAuthRepository userAuthRepository;
	private final UserSessionRepository userSessionRepository;
	private final JwtTokenProvider jwtTokenProvider;

	@Transactional
	public TokenRefreshResponseDTO tokenRefresh(TokenRefreshRequestDTO tokenRefreshRequestDTO) {
		String refreshToken = tokenRefreshRequestDTO.refreshToken();

		validateRefreshToken(refreshToken);

		Long userId = extractUserIdFromRefreshToken(refreshToken);

		UserSession userSession = userSessionRepository.findByUser_UserId(userId)
			.orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

		// request로 들어온 리프레쉬 토큰이 DB에 저장되어있는 리프레쉬 토큰과 동일한지 검증
		if (!userSession.getRefreshToken().equals(refreshToken)) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		// 유저 존재하는지 검증
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// 새로운 액세스토큰 발급
		String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());

		return new TokenRefreshResponseDTO(accessToken);
	}

	private void validateRefreshToken(String refreshToken) {
		// 위조/서명오류/만료된 리프레쉬 토큰인지 검증
		boolean valid = jwtTokenProvider.validateToken(refreshToken, true);

		if (!valid) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN); // 만료/위조/서명오류
		}
	}


	private Long extractUserIdFromRefreshToken(String refreshToken) {
		// 리프레쉬 토큰에서 UserId 추출
		try {
			return jwtTokenProvider.getUserId(refreshToken, true);
		} catch (Exception e) {
			throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}



	// 카카오 로그인
	@Transactional
	public KakaoLoginResponseDTO kakaoLogin(String code) {
		String kakaoAccessToken = kakaoOAuthProvider.getKakaoAccessToken(code);
		KakaoUserInfoResponseDTO userInfo = kakaoOAuthProvider.getKakaoUserInfo(kakaoAccessToken);

		String kakaoId = String.valueOf(userInfo.id());

		// provider = KAKAO, providerUserId인 값을 통해 이미 가입된 회원인지 식별
		Optional<UserAuth> existingUserAuth = userAuthRepository.findByProviderAndProviderUserId(KAKAO, kakaoId);

		String email = userInfo.kakaoAccount().email();

		// 신규 유저일 경우 User, UserAuth값을 새롭게 생성함
		UserAuth userAuth = existingUserAuth
			.orElseGet(() -> {
				// User 값 새롭게 생성
				User newUser = userRepository.save(User.builder()
					.email(email)
					.build());

				return userAuthRepository.save(
					UserAuth.builder()
						.user(newUser)
						.provider(KAKAO)
						.providerUserId(kakaoId)
						.build());
			});

		User user = userAuth.getUser();

		String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

		userSessionRepository.save(
			UserSession.builder()
				.user(user)
				.refreshToken(refreshToken)
				.expiresAt(LocalDateTime.now().plusDays(14))
				.build()
		);

		UserStatus userStatus = user.getUserStatus();
		NextStep nextStep = null;
		Boolean marketingConsent = user.getMarketingConsent();

		// 최초 회원가입한 소셜 로그인 유저일 경우
		if (user.getUserStatus() == UserStatus.CREATED) {
			// 최초 회원가입인 경우 TERMS 페이지로 이동,
			// 회원가입 이후 약관 동의까지 마친 경우 ONBOARDING 페이지로 이동
			nextStep = (user.getMarketingConsent() == null)
				? NextStep.TERMS
				: NextStep.ONBOARDING;
		}

		return new KakaoLoginResponseDTO(
			user.getUserId(), accessToken, refreshToken,
			userStatus, nextStep
		);
	}
}
