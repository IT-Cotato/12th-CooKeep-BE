package com.cookeep.cookeep.domain.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.user.entity.UserAuth;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
}
