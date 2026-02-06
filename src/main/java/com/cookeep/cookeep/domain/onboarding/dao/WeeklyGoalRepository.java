package com.cookeep.cookeep.domain.onboarding.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;
import com.cookeep.cookeep.domain.user.entity.User;

import java.time.LocalDate;
import java.util.Optional;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {
    Optional<WeeklyGoal> findFirstByUserAndWeekStartDateOrderByCreatedAtDesc(User user, LocalDate weekStartDate);
}
