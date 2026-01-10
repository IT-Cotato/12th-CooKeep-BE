package com.cookeep.cookeep.domain.Plants.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.Users.entity.Users;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_plants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPlants extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_plant_id")
    private Long userPlantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user; // 식물 소유자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id", nullable = false)
    private Plants plant; // 기본 식물 정보

    @Column(nullable = false)
    private Integer level; // 현재 성장 단계 (예: 1~4단계)

    @Column(name = "water_count", nullable = false)
    private Integer waterCount; // 해당 단계에서 받은 물 주기 횟수

    @Column(name = "is_harvested", nullable = false)
    private Boolean isHarvested; // 수확 완료 여부

    @Column(name = "is_frozen", nullable = false)
    private Boolean isFrozen; // 성장 정지 여부 (14일 미접속 시 true)

    @Builder
    public UserPlants(Users user, Plants plant) {
        this.user = user;
        this.plant = plant;
        this.level = 1;         // 씨앗 단계부터 시작
        this.waterCount = 0;    // 물 주기 0회부터 시작
        this.isHarvested = false;
        this.isFrozen = false;  // 기본은 활동 중
    }

    // 물 주기 및 단계 상승 체크
    // 정책: 씨앗(1회) -> 새싹(2회) -> 성장(3회) -> 수확
    public void giveWater() {
        if (this.isHarvested || this.isFrozen) return;

        this.waterCount++;

        if (canLevelUp()) {
            this.level++;
            this.waterCount = 0; // 다음 단계를 위해 초기화
        }
    }

    private boolean canLevelUp() {
        return switch (this.level) {
            case 1 -> this.waterCount >= 1; // 씨앗 -> 새싹
            case 2 -> this.waterCount >= 2; // 새싹 -> 성장
            case 3 -> this.waterCount >= 3; // 성장 -> 수확
            default -> false;
        };
    }

     // 현재 단계(level)에 맞는 이미지 URL을 반환
    public String getCurrentImageUrl() {
        return switch (this.level) {
            case 1 -> plant.getSeedImageUrl();
            case 2 -> plant.getSproutImageUrl();
            case 3 -> plant.getGrowthImageUrl();
            case 4 -> plant.getHarvestImageUrl();
            default -> throw new IllegalArgumentException("잘못된 성장 단계입니다.");
        };
    }

    // 성장 정지 처리
    public void freeze() {
        this.isFrozen = true;
    }

    public void unfreeze() {
        this.isFrozen = false;
    }
}
