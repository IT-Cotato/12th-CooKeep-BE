package com.cookeep.cookeep.domain.onboarding.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.request.AgreementRequestDTO;
import com.cookeep.cookeep.api.dto.request.OnboardingRequestDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.dao.UserFoodPreferenceRepository;
import com.cookeep.cookeep.domain.onboarding.dao.UserOnboardingRepository;
import com.cookeep.cookeep.domain.onboarding.entity.FoodType;
import com.cookeep.cookeep.domain.onboarding.entity.UserFoodPreference;
import com.cookeep.cookeep.domain.onboarding.entity.UserOnboarding;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {
	private final UserRepository userRepository;
	private final UserFoodPreferenceRepository userFoodPreferenceRepository;
	private final UserOnboardingRepository userOnboardingRepository;

	// 소셜 로그인 회원 대상 약관 동의 여부 저장
	@Transactional
	public void saveAgreement(AgreementRequestDTO agreementRequestDTO, Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// // 이미 마케팅 활용에 동의했는데 동일한 페이지에 재진입한 경우
		// if (user.getMarketingConsent() != null) {
		// 	throw new AppException(ErrorCode.MARKETING_CONSENT_ALREADY_EXISTS);
		// }

		user.setMarketingConsent(agreementRequestDTO.marketingConsent());
	}

	// 온보딩 과정에서 알림 켜기를 선택한 경우
	@Transactional
	public void agreeMarketingPush(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// 알림 켜기 버튼을 누른 사용자만 해당 api 경로로 진입하므로 true로 설정
		user.setMarketingPush(true);
	}

	@Transactional
	public void saveOnboarding(Long userId, OnboardingRequestDTO onboardingRequestDTO) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

		// 유저 온보딩값이 있는지 조회, 없다면 새로 생성
		// DTO의 값을 그대로 save하는 방식은 동일한 유저가 온보딩값을 두 번 저장하면 409 에러 발생
		// 예외가 안 나도록 값을 업데이트하는 방식으로 수정하였음
		UserOnboarding userOnboarding = userOnboardingRepository.findById(userId)
				.orElseGet(() -> UserOnboarding.builder()
					.user(user)
					.build());

		userOnboarding.update(onboardingRequestDTO.cookingLevel(), onboardingRequestDTO.weeklyGoal());

		userOnboardingRepository.save(userOnboarding);


		// userFoodPreferencerk 만약 존재한다면, 테이블 구조가 다르기 때문에 앞과는 다르게
		// update가 아니라 user의 값을 전체 삭제 후 새롭게 저장
		if (userFoodPreferenceRepository.existsByUser(user)) {
			userFoodPreferenceRepository.deleteAllByUser(user);
			userFoodPreferenceRepository.flush();
		}

		onboardingRequestDTO.favoriteFoodTypes().stream()
				.distinct()
				.forEach(foodType -> userFoodPreferenceRepository.save(
					UserFoodPreference.builder()
						.user(user)
						.foodType(foodType)
						.build()
				));
	}
}
