package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.plant.entity.PlantStatus;
import com.cookeep.cookeep.domain.plant.entity.UserPlant;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GrowingPlantResponseDto {
	private Long userPlantId;
	private String plantName;
	private Integer level;
	private Integer wateringCnt;
	private PlantStatus plantStatus;

	public static GrowingPlantResponseDto from(UserPlant userPlant, User user) {
		return GrowingPlantResponseDto.builder()
			.userPlantId(userPlant.getUserPlantId())
			.plantName(userPlant.getPlant().getPlantType().getDisplayName())
			.level(userPlant.getLevel())
			.wateringCnt(userPlant.getWaterCount())
			.plantStatus(user.getPlantStatus())
			.build();
	}
}
