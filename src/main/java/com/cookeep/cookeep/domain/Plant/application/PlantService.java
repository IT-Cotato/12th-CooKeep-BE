package com.cookeep.cookeep.domain.Plant.application;

import com.cookeep.cookeep.domain.Plant.dao.PlantRepository;
import com.cookeep.cookeep.domain.Plant.entity.Plant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlantService {
    private final PlantRepository plantRepository;

    @Transactional(readOnly = true)
    public List<Plant> getAllPlants() {
        return plantRepository.findAll();
    }
}