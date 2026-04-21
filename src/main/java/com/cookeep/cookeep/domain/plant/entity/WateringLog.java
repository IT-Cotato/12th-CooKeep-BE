package com.cookeep.cookeep.domain.plant.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "watering_logs")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WateringLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "watering_log_id")
    private Long wateringLogId;

    @Column(name = "user_plant_id")
    private Long userPlantId; // 어떤 식물에 물을 줬는지 (FK 없이 ID만 보관)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 물을 준 사용자 (랭킹 집계용)
}
