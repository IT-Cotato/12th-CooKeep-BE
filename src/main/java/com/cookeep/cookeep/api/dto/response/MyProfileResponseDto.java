package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.onboarding.entity.GoalActionType;
import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Getter
@Builder
public class MyProfileResponseDto {
    private String nickname;
    private String profilePlantImageUrl;
    private String growingPlantName;
    private Long daysSinceJoined;
    private WeeklyGoalDto weeklyGoal;

    @Getter
    @Builder
    public static class WeeklyGoalDto {
        private GoalActionType goalActionType;
        private int targetCount;
        private int currentCount;
        private boolean isAchieved;

        public static WeeklyGoalDto from(WeeklyGoal weeklyGoal) {
            return WeeklyGoalDto.builder()
                    .goalActionType(weeklyGoal.getGoalActionType())
                    .targetCount(weeklyGoal.getTargetCount())
                    .currentCount(weeklyGoal.getCurrentCount())
                    .isAchieved(weeklyGoal.isAchieved())
                    .build();
        }
    }

    public static MyProfileResponseDto of(User user, WeeklyGoal weeklyGoal, String growingPlantName) {
        String profileImageUrl = null;
        if (user.getProfilePlant() != null) {
            profileImageUrl = user.getProfilePlant().getCurrentImageUrl();
        }

        long daysSinceJoined = ChronoUnit.DAYS.between(
                user.getCreatedAt().toLocalDate(),
                LocalDate.now()
        ) + 1; // 가입 당일을 1일로 계산

        return MyProfileResponseDto.builder()
            .nickname(user.getNickname())
            .profilePlantImageUrl(profileImageUrl)
            .growingPlantName(growingPlantName)
            .daysSinceJoined(daysSinceJoined)
            .weeklyGoal(weeklyGoal != null ? WeeklyGoalDto.from(weeklyGoal) : null)
            .build();
    }
}
