package com.cookeep.cookeep.domain.user.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.user.entity.User;
import com.cookeep.cookeep.domain.user.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
	Optional<UserSession> findByUser_UserId(Long userId);
	Optional<UserSession> findByUser(User user);

	void deleteByUser_UserId(Long userId);

	boolean existsByUser_UserId(Long userId);
}
