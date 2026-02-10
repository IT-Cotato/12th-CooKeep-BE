package com.cookeep.cookeep.domain.mycookeep.application;

import com.cookeep.cookeep.api.dto.request.WeeklyGoalRequestDto;
import com.cookeep.cookeep.api.dto.response.MyProfileResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.onboarding.dao.WeeklyGoalRepository;
import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.plant.dao.UserPlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Transactional
public class MyCookeepService {

    private final UserReader userReader;
    private final WeeklyGoalRepository weeklyGoalRepository;
    private final UserPlantRepository userPlantRepository;

    @Transactional(readOnly = true)
    public MyProfileResponseDto getProfile(Long userId) {
        User user = userReader.readById(userId);

        LocalDate currentWeekStart = getCurrentWeekStart();

        WeeklyGoal weeklyGoal = weeklyGoalRepository
                .findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, currentWeekStart)
                .orElse(null);

        // 현재 키우는 식물(수확되지 않은, 얼지 않은 식물)의 이름 조회
        String growingPlantName = userPlantRepository
            .findByUserAndIsHarvestedFalseAndIsFrozenFalse(user)
            .map(userPlant -> userPlant.getPlant().getPlantType().getDisplayName())
            .orElse(null);

        return MyProfileResponseDto.of(user, weeklyGoal, growingPlantName);
    }

    public void setWeeklyGoal(Long userId, WeeklyGoalRequestDto request) {
        User user = userReader.readById(userId);

        LocalDate currentWeekStart = getCurrentWeekStart();

        // 이번 주 목표가 이미 있으면 변경 불가
        if (weeklyGoalRepository.findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(user, currentWeekStart).isPresent()) {
            throw new AppException(ErrorCode.WEEKLY_GOAL_ALREADY_EXISTS);
        }

        WeeklyGoal weeklyGoal = WeeklyGoal.builder()
                .user(user)
                .goalActionType(request.goalActionType())
                .targetCount(request.targetCount())
                .weekStartDate(currentWeekStart)
                .build();

        weeklyGoalRepository.save(weeklyGoal);
    }

    private LocalDate getCurrentWeekStart() {
        return LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
