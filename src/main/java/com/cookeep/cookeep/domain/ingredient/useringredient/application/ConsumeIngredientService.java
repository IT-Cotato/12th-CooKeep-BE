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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumeIngredientService {

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
        boolean granted = cookieService.grantDailyCookie(userId, dailyType);

        int points = granted ? dailyType.getDefaultAmount() : 0;
        List<CookieLog.CookieLogType> grantedTypes = new ArrayList<>();
        if (granted) {
            grantedTypes.add(dailyType);
        }

        // 4. 주간 소비 리포트 기록 (삭제 전에 호출해야 leftDays 읽기 가능)
        consumptionReportService.markConsumed(userId, userIngredients);

        // 5. 재료 삭제
        userIngredientRepository.deleteAll(userIngredients);

        // 6. 임박 재료(leftDays=0) 개수만큼 주간 목표(USE_EXPIRING_INGREDIENT) 카운트 증가
        long urgentCount = userIngredients.stream()
                .filter(ui -> ui.getLeftDays() == 0)
                .count();

        boolean weeklyGoalAchieved = false;
        for (int i = 0; i < urgentCount; i++) {
            // handleGoalProgress는 이미 달성된 경우 false를 반환하므로 중복 지급 없음
            if (weeklyGoalService.handleGoalProgress(userId, GoalActionType.USE_EXPIRING_INGREDIENT)) {
                weeklyGoalAchieved = true;
            }
        }

        log.info("User {} consumed {} ingredients via manual action. " +
                        "Reward granted: {}, points: {}, urgentCount: {}, weeklyGoalAchieved: {}",
                userId, userIngredients.size(), granted, points, urgentCount, weeklyGoalAchieved);

        return ConsumeIngredientsResponseDto.of(granted, points, grantedTypes, weeklyGoalAchieved);
    }

}
