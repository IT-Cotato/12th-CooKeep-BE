package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.plant.entity.Plant;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// JSON 출력 순서를 정의 (왼쪽부터 순서대로)
@JsonPropertyOrder({ "plantId", "plantName", "seedImageUrl", "sproutImageUrl", "growthImageUrl", "harvestImageUrl" })
public class PlantResponse {
    private Integer plantId;
    private String plantName; // ENUM의 displayName (예: "감자")
    private String seedImageUrl;
    private String sproutImageUrl;
    private String growthImageUrl;
    private String harvestImageUrl;

    public static PlantResponse from(Plant plant) {
        return PlantResponse.builder()
                .plantId(plant.getPlantId())
                .plantName(plant.getPlantName().getDisplayName()) // 한글 이름
                .seedImageUrl(plant.getSeedImageUrl())
                .sproutImageUrl(plant.getSproutImageUrl())
                .growthImageUrl(plant.getGrowthImageUrl())
                .harvestImageUrl(plant.getHarvestImageUrl())
                .build();
    }
}