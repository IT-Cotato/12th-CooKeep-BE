package com.cookeep.cookeep.domain.onboarding.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.onboarding.entity.UserOnboarding;

public interface UserOnboardingRepository extends JpaRepository<UserOnboarding, Long> {
}
