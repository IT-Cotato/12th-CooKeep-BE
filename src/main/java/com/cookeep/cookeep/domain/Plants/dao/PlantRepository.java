package com.cookeep.cookeep.domain.Plants.dao;

import com.cookeep.cookeep.domain.Plants.entity.Plants;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantRepository extends JpaRepository<Plants, Integer> {
}
