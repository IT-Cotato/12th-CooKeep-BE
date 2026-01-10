package com.cookeep.cookeep.domain.Plant.dao;

import com.cookeep.cookeep.domain.Plant.entity.Plant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantRepository extends JpaRepository<Plant, Integer> {
}
