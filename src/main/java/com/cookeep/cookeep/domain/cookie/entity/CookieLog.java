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

    public enum CookieLogType {
        // --- 지급 ---
        ONBOARDING_INGREDIENT,    // 최초 냉장고 재료 등록
        ONBOARDING_RECIPE,        // 최초 레시피 받기 + 소비
        DAILY_FIRST_CONSUME,      // 당일 최초 식재료 소비
        RECIPE_LOAD,              // 레시피 불러오기
        FOOD_PHOTO_REG,           // 음식 사진 등록
        WEEKLY_GOAL_ACHIEVE,      // 금주 목표 달성 보너스
        URGENT_INGREDIENT_USE,    // 유통기한 임박 재료 사용 보너스
        PLANT_HARVEST_REWARD,     // 식물 키우기 완료 보너스
        RETENTION_REWARD,         // 14일 이상 미접속 후 복귀 보너스

        // --- 차감 ---
        WATERING,                 // 식물 물 주기 (쿠키 10개 소모)
        REVIVE_PLANT              // 식물 다시 살리기 (쿠키 5개 소모)
    }
}