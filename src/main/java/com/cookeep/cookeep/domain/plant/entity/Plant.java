package com.cookeep.cookeep.domain.plant.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Plants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Plant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plant_id")
    private Integer plantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plant_name", nullable = false, length = 20)
    private PlantType plantName;

    @Column(name = "seed_image_url", nullable = false, length = 512)
    private String seedImageUrl; // 추후에 기본값 고정 예정

    @Column(name = "sprout_image_url", nullable = false, length = 512)
    private String sproutImageUrl; // 추후에 기본값 고정 예정

    @Column(name = "growth_image_url", nullable = false, length = 512)
    private String growthImageUrl;

    @Column(name = "harvest_image_url", nullable = false, length = 512)
    private String harvestImageUrl;
}