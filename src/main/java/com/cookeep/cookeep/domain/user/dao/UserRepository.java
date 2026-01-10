package com.cookeep.cookeep.domain.user.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cookeep.cookeep.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
