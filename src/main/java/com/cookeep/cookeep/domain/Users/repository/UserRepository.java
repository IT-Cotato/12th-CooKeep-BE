package com.cookeep.cookeep.domain.Users.repository;

import com.cookeep.cookeep.domain.Users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
}
