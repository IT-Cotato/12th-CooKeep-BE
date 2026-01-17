package com.cookeep.cookeep.domain.onboarding.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cookeep.cookeep.api.dto.request.AgreementRequestDTO;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OnboardingService {
	private final UserRepository userRepository;

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
}
