package com.cookeep.cookeep.domain.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.user.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
}
