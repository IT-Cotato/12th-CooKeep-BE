package com.cookeep.cookeep.domain.user.application;

import static com.cookeep.cookeep.domain.user.entity.Provider.*;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.request.LoginRequestDTO;
import com.cookeep.cookeep.api.dto.request.ResetPasswordRequestDTO;
import com.cookeep.cookeep.api.dto.request.SendCodeRequestDTO;
import com.cookeep.cookeep.api.dto.request.SignupRequestDTO;
import com.cookeep.cookeep.api.dto.request.TokenRefreshRequestDTO;
import com.cookeep.cookeep.api.dto.request.VerifyCodeRequestDTO;
import com.cookeep.cookeep.api.dto.response.SocialLoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.LoginResponseDTO;
import com.cookeep.cookeep.api.dto.response.SignUpResponseDTO;
import com.cookeep.cookeep.api.dto.response.TokenRefreshResponseDTO;
import com.cookeep.cookeep.domain.user.dto.OAuthUserInfoDTO;
import com.cookeep.cookeep.domain.user.dto.TokenPair;
import com.cookeep.cookeep.domain.user.entity.Provider;
import com.cookeep.cookeep.domain.verification.application.EmailVerificationService;
import com.cookeep.cookeep.domain.verification.entity.VerificationPurpose;
import com.cookeep.cookeep.security.JwtTokenProvider;
import com.cookeep.cookeep.domain.user.dao.UserAuthRepository;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.dao.UserSessionRepository;
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

	private final UserRepository userRepository;
	private final UserAuthRepository userAuthRepository;
	private final UserSessionRepository userSessionRepository;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserReader userReader;
	private final PasswordEncoder passwordEncoder;
	private final NicknameGenerator nicknameGenerator;
	private final EmailVerificationService emailVerificationService;

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

		user.updateLastAccessAt(LocalDateTime.now());

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
		user.updateLastAccessAt(LocalDateTime.now());

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

	// 닉네임 제약 위반 시 재시도 횟수를 제한하기 위한 값 (무한 반복 방지)
	private static final int MAX_TRIES = 30;

	private final List<OAuthProvider> oAuthProviders;

	private OAuthProvider getProvider(Provider provider) {
		return oAuthProviders.stream()
			.filter(p -> p.provider() == provider)
			.findFirst()
			.orElseThrow(() -> new RuntimeException("지원하지 않는 소셜 로그인입니다."));
		// ErrorCode가 develop 브랜치에서 대폭 수정된 상태라 추후 AppException으로 수정 예정
	}

	// 소셜 로그인
	@Transactional
	public SocialLoginResponseDTO socialLogin(Provider provider, String code, String redirectUri) {
		// provider 타입에 따라 KakaoOAuthProvider 또는 GoogleOAuthProvider 반환
		OAuthProvider oAuthProvider = getProvider(provider);
		String accessToken = oAuthProvider.getAccessToken(code, redirectUri);
		OAuthUserInfoDTO userInfo = oAuthProvider.getUserInfo(accessToken);

		String socialId = userInfo.id();

		// provider, providerUserId인 값을 통해 이미 가입된 회원인지 식별
		Optional<UserAuth> existingUserAuth = userAuthRepository.findByProviderAndProviderUserId(provider, socialId);

		String email = userInfo.email();

		// 신규 유저일 경우 User, UserAuth값을 새롭게 생성함
		UserAuth userAuth = existingUserAuth
			.orElseGet(() -> {
				// 동일한 이메일로 가입된 User가 존재하는지 확인
				// 존재하지 않을 경우 새로운 유저 생성
				User user = userRepository.findByEmail(email)
					.orElseGet(() -> createSocialUser(email));

				// 기존 유저든 신규 유저든 UserAuth가 추가됨
				return userAuthRepository.save(
					UserAuth.builder()
						.user(user)
						.provider(provider)
						.providerUserId(socialId)
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
			nextStep = (marketingConsent == null)
				? NextStep.TERMS
				: NextStep.ONBOARDING;
		}

		return new SocialLoginResponseDTO(
			user.getUserId(), tokenPair.accessToken(), tokenPair.refreshToken(),
			userStatus, nextStep
		);
	}

	private User createSocialUser(String email) {

		for (int i = 0; i < MAX_TRIES; i++) {
			String nickname = nicknameGenerator.generateRandomNickname();

			try {
				return userRepository.saveAndFlush(User.builder()
					.email(email)
					.nickname(nickname)
					.build());
			} catch (DataIntegrityViolationException e) {
				// 닉네임 관련 제약 위반인 경우에만 재시도
				if (shouldRetryNickname(e)) {
					log.debug("Nickname conflict during social signup. try={}/{}", i + 1, MAX_TRIES);
					continue;
				}
				// 그 외 제약 위반은 에러 발생
				log.error("Signup failed due to integrity violation (non-nickname).", e);
				throw e;
			}
		}

		log.warn("Failed to generate unique nickname after {} tries (social signup).", MAX_TRIES);
		throw new AppException(ErrorCode.NICKNAME_GENERATION_UNAVAILABLE);
	}

	@Transactional
	public void sendSignupCode(SendCodeRequestDTO sendCodeRequestDTO) {
		String email = sendCodeRequestDTO.email();

		if (userRepository.existsByEmail(email)) {
			// 이미 가입된 이메일일 경우
			throw new AppException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
		}
		emailVerificationService.sendCode(email, VerificationPurpose.SIGNUP);
	}

	@Transactional
	public void sendPasswordResetCode(SendCodeRequestDTO sendCodeRequestDTO) {
		String email = sendCodeRequestDTO.email();

		if (!userRepository.existsByEmail(email)) {
			// 가입되지 않은 이메일일 경우
			throw new AppException(ErrorCode.EMAIL_NOT_REGISTERED);
		}

		emailVerificationService.sendCode(email, VerificationPurpose.RESET_PASSWORD);
	}

	@Transactional
	public void verifySignupCode(VerifyCodeRequestDTO verifyCodeRequestDTO) {
		String email = verifyCodeRequestDTO.email();
		String code = verifyCodeRequestDTO.code();

		emailVerificationService.verifyCode(email, VerificationPurpose.SIGNUP, code);
	}

	@Transactional
	public void verifyPasswordResetCode(VerifyCodeRequestDTO verifyCodeRequestDTO) {
		String email = verifyCodeRequestDTO.email();
		String code = verifyCodeRequestDTO.code();

		emailVerificationService.verifyCode(email, VerificationPurpose.RESET_PASSWORD, code);
	}

	@Transactional
	public SignUpResponseDTO signUp(SignupRequestDTO signupRequestDTO) {
		String email = signupRequestDTO.email();

		// 이메일 중복 검증 진행
		checkEmail(email);

		String encodedPassword = passwordEncoder.encode(signupRequestDTO.password());

		Boolean marketingConsent = signupRequestDTO.marketingConsent();

		User user = null;

		for (int i = 0; i < MAX_TRIES; i++) {

			String nickname = nicknameGenerator.generateRandomNickname();

			try {
				user = userRepository.saveAndFlush(User.builder()
					.email(email)
					.password(encodedPassword)
					.marketingConsent(marketingConsent)
					.nickname(nickname)
					.build());

				break; // 중복 없이 저장 성공한 경우
			} catch (DataIntegrityViolationException e) {
				// 닉네임 관련 제약 위반일 경우에만 재시도
				if (shouldRetryNickname(e)) {
					log.debug("Nickname conflict during signup. try={}/{}", i + 1, MAX_TRIES);
					continue;
				}
				// 그 외 제약 위반은 에러 발생
				log.error("Signup failed due to integrity violation (non-nickname).", e);
				throw e;
			}
		}

		if (user == null) {
			// MAX_TRIES를 초과해서 실패하는 경우
			log.error("Failed to generate unique nickname after {} tries.", MAX_TRIES);
			throw new AppException(ErrorCode.NICKNAME_GENERATION_UNAVAILABLE);
		}

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

	private boolean shouldRetryNickname(DataIntegrityViolationException e) {
		// DataIntegrityViolationException 중 닉네임 관련 제약 위반인지 식별
		Throwable t = e;
		while (t != null) { // 예외 체인 내에서 DB 에러 메세지를 탐색
			String message = t.getMessage();
			if (message != null && message.contains("nickname")) {
				// 닉네임 관련 제약 위반으로 판단
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	private void checkEmail(String email) {
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) return; // 중복된 이메일이 아닐 경우 null 반환

		List<UserAuth> auths = userAuthRepository.findAllByUser(user);
		if (auths.isEmpty()) {
			throw new AppException(ErrorCode.USERAUTH_DOES_NOT_EXIST);
		}

		// 어떤 provider로 가입된 이메일인지 판별하기 위해
		EnumSet<Provider> providerSet = auths.stream()
			.map(UserAuth::getProvider)
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(Provider.class)));

		// LOCAL 가입자인 경우
		// LOCAL+KAKAO, LOCAL+GOOGLE은 불가능하므로 LOCAL인 경우는 단독으로 처리
		if (providerSet.contains(Provider.LOCAL)) {
			throw new AppException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
		}

		// 소셜 가입자인 경우 이메일도 함께 반환해야 하므로 ErrorCode 매핑만 함
		Map<Set<Provider>, ErrorCode> socialProviderErrorMap = Map.of(
			EnumSet.of(Provider.KAKAO), ErrorCode.USER_EMAIL_REGISTERED_WITH_KAKAO,
			EnumSet.of(Provider.GOOGLE), ErrorCode.USER_EMAIL_REGISTERED_WITH_GOOGLE,
			EnumSet.of(Provider.KAKAO, Provider.GOOGLE), ErrorCode.USER_EMAIL_REGISTERED_WITH_KAKAO_GOOGLE
		);

		ErrorCode errorCode = socialProviderErrorMap.get(providerSet);
		if (errorCode != null) {
			throw new AppException(errorCode);
		}
	}

	@Transactional
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		String email = loginRequestDTO.email();
		String password = loginRequestDTO.password();

		// 이메일 기반으로 유저 조회, 없을 경우 EMAIL_NOT_REGISTERED
		User user = userRepository.findByEmail(email)
			.orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_REGISTERED));

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

	@Transactional
	public void resetPassword(ResetPasswordRequestDTO resetPasswordRequestDTO) {

		// 인증 완료 여부 확인
		emailVerificationService.assertVerified(resetPasswordRequestDTO.email(), VerificationPurpose.RESET_PASSWORD);

		User user = userRepository.findByEmail(resetPasswordRequestDTO.email())
			.orElseThrow(() ->  new AppException(ErrorCode.EMAIL_NOT_REGISTERED));

		String encodedPassword = passwordEncoder.encode(resetPasswordRequestDTO.password());

		// 기존에 등록되어 있던 비밀번호와 새로 들어온 비밀번호가 동일할 경우
		if (passwordEncoder.matches(resetPasswordRequestDTO.password(), user.getPassword())) {
			throw new AppException(ErrorCode.SAME_AS_PREVIOUS_PASSWORD);
		}

		user.updatePassword(encodedPassword);
	}

	@Transactional
	public void logout(Long userId) {
		// 로그아웃 시 refreshToken 저장된 userSession 폐기
		userSessionRepository.deleteByUser_UserId(userId);
	}

	@Transactional
	public void withdraw(Long userId) {
		User user = userReader.readById(userId);

		// 멱등성 보장을 위해 이미 탈퇴된 회원은 성공 처리
		if (user.getUserStatus() == UserStatus.WITHDRAWN) {
			userSessionRepository.deleteByUser_UserId(userId);
			return;
		}

		user.withdraw();

		// 탈퇴 시 refreshToken도 함께 무효화
		userSessionRepository.deleteByUser_UserId(userId);
	}
}