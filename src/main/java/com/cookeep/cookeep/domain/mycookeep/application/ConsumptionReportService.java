package com.cookeep.cookeep.domain.mycookeep.application;

import com.cookeep.cookeep.api.dto.response.ConsumptionReportResponseDto;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.dao.WeeklyIngredientLogRepository;
import com.cookeep.cookeep.domain.mycookeep.entity.WeeklyIngredientLog;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsumptionReportService {

    private final WeeklyIngredientLogRepository weeklyIngredientLogRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final UserReader userReader;

    public static final int NEAR_EXPIRY_THRESHOLD = 3;

    @Transactional
    public ConsumptionReportResponseDto getReport(Long userId) {
        User user = userReader.readById(userId);
        LocalDate weekStart = getCurrentWeekStart();

        ensureWeeklyLogsExist(user, weekStart);

        int total = weeklyIngredientLogRepository
                .countTotalByUserAndWeek(userId, weekStart);
        int consumed = weeklyIngredientLogRepository
                .countConsumedByUserAndWeek(userId, weekStart);
        int nearExpiry = weeklyIngredientLogRepository
                .countNearExpiryByUserAndWeek(userId, weekStart);
        int consumedNearExpiry = weeklyIngredientLogRepository
                .countConsumedNearExpiryByUserAndWeek(userId, weekStart);

        int consumptionRate = total == 0 ? 0 : consumed * 100 / total;
        int nearExpiryRate = nearExpiry == 0 ? 0 : consumedNearExpiry * 100 / nearExpiry;

        return ConsumptionReportResponseDto.builder()
                .totalIngredientCount(total)
                .consumedIngredientCount(consumed)
                .consumptionRate(consumptionRate)
                .nearExpiryIngredientCount(nearExpiry)
                .consumedNearExpiryCount(consumedNearExpiry)
                .nearExpiryConsumptionRate(nearExpiryRate)
                .build();
    }

    @Transactional
    public void createLogForNewIngredient(User user, UserIngredient ingredient) {
        LocalDate weekStart = getCurrentWeekStart();
        boolean nearExpiry = ingredient.getLeftDays() <= NEAR_EXPIRY_THRESHOLD;

        WeeklyIngredientLog logEntry = WeeklyIngredientLog.builder()
                .user(user)
                .weekStartDate(weekStart)
                .userIngredientId(ingredient.getIngredientId())
                .everNearExpiry(nearExpiry)
                .build();

        weeklyIngredientLogRepository.save(logEntry);
    }

    @Transactional
    public void markConsumed(Long userId, List<UserIngredient> ingredients) {
        LocalDate weekStart = getCurrentWeekStart();
        List<Long> ingredientIds = ingredients.stream()
                .map(UserIngredient::getIngredientId)
                .toList();

        List<WeeklyIngredientLog> logs = weeklyIngredientLogRepository
                .findAllByUser_UserIdAndWeekStartDateAndUserIngredientIdIn(
                        userId, weekStart, ingredientIds);

        Map<Long, WeeklyIngredientLog> logMap = logs.stream()
                .collect(Collectors.toMap(
                        WeeklyIngredientLog::getUserIngredientId,
                        Function.identity()));

        for (UserIngredient ui : ingredients) {
            WeeklyIngredientLog logEntry = logMap.get(ui.getIngredientId());
            boolean isNearExpiry = ui.getLeftDays() <= NEAR_EXPIRY_THRESHOLD;

            if (logEntry != null) {
                logEntry.markConsumed(isNearExpiry);
            } else {
                log.info("Creating ad-hoc consumption log for ingredient {}",
                        ui.getIngredientId());
                WeeklyIngredientLog newLog = WeeklyIngredientLog.builder()
                        .user(ui.getUser())
                        .weekStartDate(weekStart)
                        .userIngredientId(ui.getIngredientId())
                        .everNearExpiry(isNearExpiry)
                        .build();
                newLog.markConsumed(isNearExpiry);
                weeklyIngredientLogRepository.save(newLog);
            }
        }
    }

    @Transactional
    public void updateNearExpiryFlags(List<Long> nearExpiryIngredientIds,
                                       LocalDate weekStart) {
        if (nearExpiryIngredientIds.isEmpty()) return;

        List<WeeklyIngredientLog> logs = weeklyIngredientLogRepository
                .findNotYetNearExpiryByWeekAndIngredientIds(
                        weekStart, nearExpiryIngredientIds);

        for (WeeklyIngredientLog logEntry : logs) {
            logEntry.markEverNearExpiry();
        }
    }

    @Transactional
    public void createWeeklySnapshot(User user, List<UserIngredient> ingredients,
                                      LocalDate weekStart) {
        List<WeeklyIngredientLog> logs = ingredients.stream()
                .map(ui -> WeeklyIngredientLog.builder()
                        .user(user)
                        .weekStartDate(weekStart)
                        .userIngredientId(ui.getIngredientId())
                        .everNearExpiry(ui.getLeftDays() <= NEAR_EXPIRY_THRESHOLD)
                        .build())
                .toList();

        weeklyIngredientLogRepository.saveAll(logs);
    }

    private void ensureWeeklyLogsExist(User user, LocalDate weekStart) {
        boolean exists = weeklyIngredientLogRepository
                .existsByUser_UserIdAndWeekStartDate(user.getUserId(), weekStart);

        if (!exists) {
            log.info("No weekly logs found for user {} week {}, creating lazily",
                    user.getUserId(), weekStart);
            List<UserIngredient> currentIngredients =
                    userIngredientRepository.findAllByUser_UserId(user.getUserId());
            if (!currentIngredients.isEmpty()) {
                createWeeklySnapshot(user, currentIngredients, weekStart);
            }
        }
    }

    private LocalDate getCurrentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
