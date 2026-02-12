package com.cookeep.cookeep.domain.mycookeep.application;

import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeeklyIngredientLogScheduler {

    private final UserRepository userRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final ConsumptionReportService consumptionReportService;

    /**
     * 매주 월요일 00:00:05에 모든 사용자의 현재 식재료 스냅샷을 생성한다.
     * 기존 leftDays 스케줄러(00:00:00)보다 5초 뒤에 실행하여 leftDays가 먼저 갱신되도록 한다.
     */
    @Scheduled(cron = "5 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void createWeeklySnapshots() {
        log.info("=== Starting weekly ingredient log snapshot ===");
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        try {
            List<User> allUsers = userRepository.findAll();
            int totalCount = 0;

            for (User user : allUsers) {
                List<UserIngredient> ingredients =
                        userIngredientRepository.findAllByUser_UserId(user.getUserId());

                if (!ingredients.isEmpty()) {
                    consumptionReportService
                            .createWeeklySnapshot(user, ingredients, weekStart);
                    totalCount += ingredients.size();
                }
            }

            log.info("=== Created weekly snapshots: {} logs for {} users ===",
                    totalCount, allUsers.size());
        } catch (Exception e) {
            log.error("=== Error creating weekly snapshots ===", e);
        }
    }
}
