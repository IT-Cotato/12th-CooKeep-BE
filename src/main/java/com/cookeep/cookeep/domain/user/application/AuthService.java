package com.cookeep.cookeep.domain.user.application;

import static com.cookeep.cookeep.domain.user.entity.Provider.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.response.KakaoLoginResponseDTO;
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
		if (userStatus == UserStatus.CREATED) {
			nextStep = NextStep.TERMS;
		}

		return new KakaoLoginResponseDTO(
			user.getUserId(), accessToken, refreshToken,
			userStatus, nextStep
		);
	}
}
