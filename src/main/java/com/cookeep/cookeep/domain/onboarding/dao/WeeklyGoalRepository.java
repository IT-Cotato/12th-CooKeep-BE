package com.cookeep.cookeep.domain.onboarding.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.onboarding.entity.WeeklyGoal;

public interface WeeklyGoalRepository extends JpaRepository<WeeklyGoal, Long> {
}
