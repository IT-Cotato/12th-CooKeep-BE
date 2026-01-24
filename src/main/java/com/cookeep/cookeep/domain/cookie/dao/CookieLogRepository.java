package com.cookeep.cookeep.domain.cookie.dao;

import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CookieLogRepository extends JpaRepository<CookieLog, Long> {

}