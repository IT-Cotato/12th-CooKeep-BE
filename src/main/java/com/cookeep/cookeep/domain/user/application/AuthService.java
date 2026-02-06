package com.cookeep.cookeep.domain.user.application;

import static com.cookeep.cookeep.domain.user.entity.Provider.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.response.KakaoLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.domain.user.dto.TokenPair;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.security.JwtTokenProvider;
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
	private final UserReader userReader;
	private final PasswordEncoder passwordEncoder;

	// 액세스 토큰이 만료되었을 경우 리프레쉬 토큰으로 액세스 토큰 갱신
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

		User user = userReader.readById(userId);

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

	private TokenPair issueTokensAndUpsertSession(User user) {
		// 액세스 토큰, 리프레쉬 토큰 발급
		String accessToken = jwtTokenProvider.createAccessToken(user.getUserId());
		String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

		// userSession 존재하는지 조회, 없으면 생성
		UserSession userSession = userSessionRepository.findByUser(user)
			.orElseGet(() -> UserSession.builder()
				.user(user)
				.build());

		userSession.update(refreshToken, LocalDateTime.now().plusDays(14));

		userSessionRepository.save(userSession);

		return new TokenPair(accessToken, refreshToken);
	}

	// 카카오 로그인
	@Transactional
	public KakaoLoginResponseDTO kakaoLogin(String code, String redirectUri) {
		String kakaoAccessToken = kakaoOAuthProvider.getKakaoAccessToken(code, redirectUri);
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

		// 액세스 토큰, 리프레쉬 토큰 발급
		TokenPair tokenPair = issueTokensAndUpsertSession(user);

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
			user.getUserId(), tokenPair.accessToken(), tokenPair.refreshToken(),
			userStatus, nextStep
		);
	}

	@Transactional
	public SignUpResponseDTO signUp(SignupRequestDTO signupRequestDTO) {

		String phoneNumber = signupRequestDTO.phoneNumber();

		if (userRepository.existsByPhoneNumber(phoneNumber)) {
			// 이미 등록된 전화번호일 경우
			// SMS 인증에서 1차 검증을 하지만, api 직접 호출 등을 고려해 회원가입 api에서도 중복을 검토하도록 하였음
			throw new AppException(ErrorCode.USER_PHONE_ALREADY_EXISTS);
		}

		String email = signupRequestDTO.email();

		// 이미 등록된 이메일인지 확인
		checkEmail(email);

		String encodedPassword = passwordEncoder.encode(signupRequestDTO.password());

		Boolean marketingConsent = signupRequestDTO.marketingConsent();

		User user = userRepository.save(User.builder()
			.phoneNumber(phoneNumber)
			.email(email)
			.password(encodedPassword)
			.marketingConsent(marketingConsent)
			.build());

		// 액세스 토큰, 리프레쉬 토큰 발급
		TokenPair tokenPair = issueTokensAndUpsertSession(user);

		userAuthRepository.save(
			UserAuth.builder()
				.user(user)
				.provider(LOCAL)
				.build());

		return new SignUpResponseDTO(
			user.getUserId(), tokenPair.accessToken(), tokenPair.refreshToken()
		);
	}

	private void checkEmail(String email) {
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) return;

		List<UserAuth> auths = userAuthRepository.findAllByUser(user);
		if (auths.isEmpty()) {
			throw new IllegalStateException("UserAuth가 존재하지 않는 User입니다. 서버에 문의해주세요.");
		}

		boolean hasLocal = auths.stream()
			.anyMatch(a -> a.getProvider() == Provider.LOCAL);

		if (hasLocal) {
			// LOCAL 사용자로 이미 등록된 이메일일 경우
			throw new AppException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
		}

		// LOCAL이 아니라 소셜로만 가입된 이메일일 경우
		throw new AppException(ErrorCode.USER_EMAIL_REGISTERED_WITH_SOCIAL);
	}

	@Transactional
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		String phoneNumber = loginRequestDTO.phoneNumber();
		String password = loginRequestDTO.password();

		// 전화번호 기반으로 유저 조회, 없을 경우 AUTH_PHONE_NOT_REGISTERED
		User user = userRepository.findByPhoneNumber(phoneNumber)
			.orElseThrow(() -> new AppException(ErrorCode.AUTH_PHONE_NOT_REGISTERED));

		// 비밀번호가 틀렸을 경우
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new AppException(ErrorCode.AUTH_PASSWORD_MISMATCH);
		}

		TokenPair tokenPair = issueTokensAndUpsertSession(user);

		UserStatus userStatus = user.getUserStatus();

		return new LoginResponseDTO(
			user.getUserId(), tokenPair.accessToken(), tokenPair.refreshToken(), userStatus
		);
	}
}
