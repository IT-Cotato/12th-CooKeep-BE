package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.Plant.entity.UserPlant;
import com.cookeep.cookeep.domain.Users.entity.Users;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPlantResponse {
    private Long userPlantId;     // 식물 식별자 (프로필 변경 API 호출 시 필요)
    private String plantName;      // 식물 이름 (예: 감자, 토마토)
    private String imageUrl;       // 현재 단계에 맞는 이미지 URL
    private Integer level;         // 성장 단계 (1~4)
    private Boolean isHarvested;   // 수확 여부 (false면 '키우는 중', true면 '수확 완료')
    private Boolean isProfile;     // 현재 이 식물이 유저의 프로필 식물인지 여부
    private String createdAt;      // 언제부터 키웠는지 (BaseEntity 활용)

    public static MyPlantResponse from(UserPlant userPlant, Users user) {
        // 유저의 profile_plant_id와 현재 식물의 ID가 같으면 true
        boolean isProfile = user.getProfilePlant() != null &&
                user.getProfilePlant().getUserPlantId().equals(userPlant.getUserPlantId());

        return MyPlantResponse.builder()
                .userPlantId(userPlant.getUserPlantId())
                .plantName(userPlant.getPlant().getPlantName().getDisplayName())
                .imageUrl(userPlant.getCurrentImageUrl())
                .level(userPlant.getLevel())
                .isHarvested(userPlant.getIsHarvested())
                .isProfile(isProfile)
                .createdAt(userPlant.getCreatedAt().toLocalDate().toString())
                .build();
    }
}