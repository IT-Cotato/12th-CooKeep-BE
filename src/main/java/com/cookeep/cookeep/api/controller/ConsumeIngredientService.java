package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.ConsumeIngredientsRequestDto;
import com.cookeep.cookeep.api.dto.response.ConsumeIngredientsResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.dao.CookieLogRepository;
import com.cookeep.cookeep.domain.cookie.dao.DailyCookieGrantRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.DailyCookieGrant;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumeIngredientService {

    private final UserIngredientRepository userIngredientRepository;
    private final UserRepository userRepository;
    private final CookieLogRepository cookieLogRepository;
    private final DailyCookieGrantRepository dailyCookieGrantRepository;


    // 식재료 섭취 완료
    @Transactional
    public ConsumeIngredientsResponseDto consumeIngredients(
            Long userId,
            ConsumeIngredientsRequestDto request
    ) {
        List<Long> ids = request.getUserIngredientIds();

        // 1. 요청 검증
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_DELETE_REQUEST);
        }

        // 2. 사용자 소유 재료 조회
        List<UserIngredient> userIngredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(ids, userId);

        if (userIngredients.isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 일부만 존재하는 경우
        if (userIngredients.size() != ids.size()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 3. 리워드 처리
        RewardResult rewardResult = processRewards(userId, userIngredients);

        // 4. 재료 삭제
        userIngredientRepository.deleteAll(userIngredients);

        return ConsumeIngredientsResponseDto.of(
                rewardResult.isGranted(),
                rewardResult.getTotalPoints(),
                rewardResult.getGrantedTypes()
        );
    }

    // --- 내부 메서드 ---

    private RewardResult processRewards(Long userId, List<UserIngredient> ingredients) {
        LocalDate today = LocalDate.now();

        int totalPoints = 0;
        boolean granted = false;
        List<CookieLog.CookieLogType> grantedTypes = new ArrayList<>();

        // 기본 일일 소비 리워드 확인
        CookieLog.CookieLogType dailyType = CookieLog.CookieLogType.BASIC_DAILY_FIRST_CONSUME;
        boolean alreadyGrantedDaily = dailyCookieGrantRepository.existsByUser_UserIdAndGrantTypeAndGrantDate(
                userId, dailyType, today
        );

        if (!alreadyGrantedDaily) {
            grantCookie(userId, dailyType, today);
            totalPoints += dailyType.getDefaultAmount();
            granted = true;
            grantedTypes.add(dailyType);
        }

        return new RewardResult(granted, totalPoints, grantedTypes);
    }

    private void grantCookie(Long userId, CookieLog.CookieLogType type, LocalDate grantDate) {
        // 1. 유저 조회 (비관적 락)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        int amount = type.getDefaultAmount();

        // 2. 쿠키 업데이트
        user.updateCookieCnt(amount);

        // 3. 쿠키 로그 저장
        CookieLog log = CookieLog.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
        cookieLogRepository.save(log);

        // 4. 일일 지급 기록 저장
        DailyCookieGrant grant = DailyCookieGrant.builder()
                .user(user)
                .grantType(type)
                .grantDate(grantDate)
                .build();
        dailyCookieGrantRepository.save(grant);
    }

    @lombok.Value
    private static class RewardResult {
        boolean granted;
        int totalPoints;
        List<CookieLog.CookieLogType> grantedTypes;
    }

}
