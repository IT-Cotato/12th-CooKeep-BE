package com.cookeep.cookeep.domain.cookie.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "Cookie_Logs")
public class CookieLog extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cookieLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int amount; // 증감분 (예: -1, +5)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CookieLogType type;

    @Getter
    public enum CookieLogType {
        // --- 지급 ---
        ONBOARDING_INGREDIENT(1),          // 최초 냉장고 재료 등록

        BASIC_DAILY_FIRST_CONSUME(1),      // 냉장고 재료 직접 소비 (당일 최초)
        BASIC_LOAD_RECIPE(1),              // 마이쿠킵에 레시피 등록
        BASIC_FOOD_PHOTO_REG(1),           // 음식 사진 등록

        BONUS_WEEKLY_GOAL_ACHIEVE(1),      // 주간 목표 달성
        BONUS_URGENT_INGREDIENT_USE(3),    // 유통기한 임박 재료 레시피 + 소비
        BONUS_PLANT_HARVEST_REWARD(15),    // 식물 키우기 완료 보너스
        BONUS_RETENTION_REWARD(1),         // 14일 이상 미접속 후 복귀 보너스

        // --- 차감 ---
        WATERING(10),                      // 식물 물 주기 (쿠키 10개 소모)
        REVIVE_PLANT(5);                   // 식물 다시 살리기 (쿠키 5개 소모)

        private final int defaultAmount;

        CookieLogType(int defaultAmount) {
            this.defaultAmount = defaultAmount;
        }
    }
}