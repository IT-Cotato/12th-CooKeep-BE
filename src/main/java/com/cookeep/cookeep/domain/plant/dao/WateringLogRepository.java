package com.cookeep.cookeep.domain.plant.dao;

import com.cookeep.cookeep.domain.plant.entity.WateringLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WateringLogRepository extends JpaRepository<WateringLog, Long> {
}
