package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.ConsumeIngredientsRequestDto;
import com.cookeep.cookeep.api.dto.response.ConsumeIngredientsResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.onboarding.application.WeeklyGoalService;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumeIngredientService {

    private static final int URGENT = 0;

    private final UserIngredientRepository userIngredientRepository;
    private final CookieService cookieService;
    private final ConsumptionReportService consumptionReportService;
    private final WeeklyGoalService weeklyGoalService;

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

        // 3. 기본 일일 소비 리워드 처리
        CookieLog.CookieLogType dailyType = CookieLog.CookieLogType.BASIC_DAILY_FIRST_CONSUME;
        boolean dailyGranted = cookieService.grantDailyCookie(userId, dailyType);

        int points = dailyGranted ? dailyType.getDefaultAmount() : 0;
        List<CookieLog.CookieLogType> grantedTypes = new ArrayList<>();
        if (dailyGranted) {
            grantedTypes.add(dailyType);
        }

        // 4. 주간 소비 리포트 기록 (삭제 전에 호출해야 leftDays 읽기 가능)
        consumptionReportService.markConsumed(userId, userIngredients);

        // 5. 임박 재료(leftDays=0) 개수만큼 주간 목표(USE_EXPIRING_INGREDIENT) 카운트 증가
        long urgentCount = userIngredients.stream()
                .filter(ui -> ui.getLeftDays() == URGENT)
                .count();

        // 6. 재료 삭제
        userIngredientRepository.deleteAll(userIngredients);

        // 7. 임박 재료 1일 1회 쿠키 지급 (BONUS_URGENT_INGREDIENT_USE)
        if (urgentCount > 0) {
            CookieLog.CookieLogType urgentType = CookieLog.CookieLogType.BONUS_URGENT_INGREDIENT_USE;
            boolean urgentGranted = cookieService.grantDailyCookie(userId, urgentType);
            if (urgentGranted) {
                grantedTypes.add(urgentType);
                points += urgentType.getDefaultAmount();
            }
        }

        // 8. 임박 재료 개수만큼 주간 목표(USE_EXPIRING_INGREDIENT) 카운트 증가
        boolean weeklyGoalAchieved = false;
        for (int i = 0; i < urgentCount; i++) {
            // handleGoalProgress는 이미 달성된 경우 false를 반환하므로 중복 지급 없음
            if (weeklyGoalService.handleGoalProgress(userId, GoalActionType.USE_EXPIRING_INGREDIENT)) {
                weeklyGoalAchieved = true;
            }
        }

        // 쿠키 타입 기록 (주간 목표 달성 쿠키는 WeeklyGoalService 내부에서 지급)
        if (weeklyGoalAchieved) {
            grantedTypes.add(CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
            points += CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE.getDefaultAmount();
        }

        log.info("User {} consumed {} ingredients. dailyGranted: {}, urgentCount: {}, weeklyGoalAchieved: {}",
                userId, userIngredients.size(), dailyGranted, urgentCount, weeklyGoalAchieved);

        return ConsumeIngredientsResponseDto.of(!grantedTypes.isEmpty(), points, grantedTypes, weeklyGoalAchieved);
    }

}
