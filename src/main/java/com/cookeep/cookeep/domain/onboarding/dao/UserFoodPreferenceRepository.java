package com.cookeep.cookeep.domain.onboarding.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cookeep.cookeep.domain.onboarding.entity.UserFoodPreference;
import com.cookeep.cookeep.domain.user.entity.User;

public interface UserFoodPreferenceRepository extends JpaRepository<UserFoodPreference, Long> {
	boolean existsByUser_UserId(Long userId);

	@Modifying
	@Query("delete from UserFoodPreference p where p.user.userId = :userId")
	int deleteAllByUserId(@Param("userId") Long userId);
}