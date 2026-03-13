package com.cookeep.cookeep.domain.onboarding.application;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.onboarding.dao.WeeklyGoalRepository;
import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WeeklyGoalService {

    private final WeeklyGoalRepository weeklyGoalRepository;
    private final UserReader userReader;
    private final CookieService cookieService;

    /**
     * 해당 액션 타입의 주간 목표가 있다면 진행 카운트를 1 증가시키고,
     * 이번 호출로 처음 달성된 경우 쿠키를 지급한다.
     *
     * @return true: 이번 호출로 목표를 달성함 (쿠키 지급됨), false: 달성 아님
     */
    public boolean handleGoalProgress(Long userId, GoalActionType actionType) {
        User user = userReader.readById(userId);
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        Optional<WeeklyGoal> goalOpt = weeklyGoalRepository
                .findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart);

        if (goalOpt.isEmpty()) return false;

        WeeklyGoal goal = goalOpt.get();
        if (!goal.getGoalActionType().equals(actionType)) return false;
        if (goal.isAchieved()) return false;

        goal.incrementCount();

        if (goal.isAchieved()) {
            cookieService.updateCookie(userId, CookieLog.CookieLogType.BONUS_WEEKLY_GOAL_ACHIEVE);
            return true;
        }
        return false;
    }

    /**
     * 액션 취소 시 주간 목표 카운트를 1 감소시킨다.
     * 이미 달성된 경우 아무 동작도 하지 않는다 (쿠키 회수 없음).
     */
    public void handleGoalUndo(Long userId, GoalActionType actionType) {
        User user = userReader.readById(userId);
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        weeklyGoalRepository
                .findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, weekStart)
                .ifPresent(goal -> {
                    if (!goal.getGoalActionType().equals(actionType)) return;
                    goal.decrementCount();
                });
    }
}
