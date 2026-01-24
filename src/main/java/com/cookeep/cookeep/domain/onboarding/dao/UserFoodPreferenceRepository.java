package com.cookeep.cookeep.domain.onboarding.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.onboarding.entity.UserFoodPreference;
import com.cookeep.cookeep.domain.user.entity.User;

public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {
	Boolean existsByUser(User user);
	void deleteAllByUser(User user);
}
