package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.notification.dto.PushNotificationEligibilityResponseDto;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// 팝업(모달) 알림 실제 전송
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopupNotificationEligibilityService {

    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final UserReader userReader;

    // 유통기한 당일(D-0) 식재료 존재 여부 확인 및 팝업 표시 자격 판단
    public PushNotificationEligibilityResponseDto checkEligibility(Long userId) {

        // 1. 유저 조회
        User user = userReader.readById(userId);

        // 2. 마케팅 수신 동의 여부 확인
        if (!Boolean.TRUE.equals(user.getMarketingConsent())) {
            return PushNotificationEligibilityResponseDto.notEligible();
        }

        // 3. D-0 재료 존재 여부 확인
        LocalDate today = LocalDate.now();

        boolean hasExpiringToday =
                userIngredientRepository.existsByUserIdAndExpirationDate(userId, today);

        if (!hasExpiringToday) {
            return PushNotificationEligibilityResponseDto.notEligible();
        }

        // 4. eligible: true로 응답 반환
        return PushNotificationEligibilityResponseDto.eligible();
    }

}
