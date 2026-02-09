package com.cookeep.cookeep.domain.notification.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.notification.dto.PushNotificationEligibilityResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

// 정기 푸시 알림 스케줄러
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationScheduler {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    public PushNotificationEligibilityResponseDto checkEligibility(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 1. 유통기한 당일(D-0) 식재료 존재 여부 확인
        LocalDate today = LocalDate.now();

        boolean hasExpiringToday = userIngredientRepository.existsByUserIdAndExpirationDate(
                userId, today
        );

        // 2. 당일 만료 식재료가 없으면 eligible: false 반환
        if (!hasExpiringToday) {
            log.info("User {} has no expiring ingredients today", userId);
            return PushNotificationEligibilityResponseDto.notEligible();
        }

        // 3. 당일 만료 식재료가 있으면 상세 정보 조회
        List<UserIngredient> expiringIngredients = userIngredientRepository
                .findByUserIdAndExpirationDateOrderByIngredientIdAsc(userId, today);

        // 4. DTO 변환 (최대 5개만 반환)
        List<PushNotificationEligibilityResponseDto.IngredientInfo> ingredientInfos =
                expiringIngredients.stream()
                        .limit(5)
                        .map(this::toIngredientInfo)
                        .collect(Collectors.toList());

        log.info("User {} has {} expiring ingredients today", userId, expiringIngredients.size());

        // 5. eligible: true로 응답 반환
        return PushNotificationEligibilityResponseDto.eligible(
                expiringIngredients.size(),
                ingredientInfos
        );
    }

    /**
     * UserIngredient를 IngredientInfo DTO로 변환
     */
    private PushNotificationEligibilityResponseDto.IngredientInfo toIngredientInfo(
            UserIngredient userIngredient
    ) {
        String ingredientName = getIngredientName(userIngredient);

        return PushNotificationEligibilityResponseDto.IngredientInfo.builder()
                .userIngredientId(userIngredient.getIngredientId())
                .name(ingredientName)
                .expiresInDays(userIngredient.getLeftDays())
                .build();
    }

    /**
     * UserIngredient의 타입(DEFAULT/CUSTOM)에 따라 식재료 이름 조회
     */
    private String getIngredientName(UserIngredient userIngredient) {
        if (userIngredient.getType() == Type.DEFAULT) {
            DefaultIngredient defaultIngredient = defaultIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .orElse(null);
            return defaultIngredient != null ? defaultIngredient.getIngredient() : "Unknown";
        } else {
            CustomIngredient customIngredient = customIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .orElse(null);
            return customIngredient != null ? customIngredient.getName() : "Unknown";
        }
    }

}
