package com.cookeep.cookeep.domain.mycookeep.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "weekly_ingredient_logs",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "week_start_date", "user_ingredient_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyIngredientLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "user_ingredient_id", nullable = false)
    private Long userIngredientId;

    @Column(name = "ever_near_expiry", nullable = false)
    private boolean everNearExpiry;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "near_expiry_when_consumed", nullable = false)
    private boolean nearExpiryWhenConsumed;

    @Builder
    public WeeklyIngredientLog(User user, LocalDate weekStartDate,
                                Long userIngredientId, boolean everNearExpiry) {
        this.user = user;
        this.weekStartDate = weekStartDate;
        this.userIngredientId = userIngredientId;
        this.everNearExpiry = everNearExpiry;
        this.consumed = false;
        this.nearExpiryWhenConsumed = false;
    }

    public void markEverNearExpiry() {
        this.everNearExpiry = true;
    }

    public void markConsumed(boolean isNearExpiry) {
        this.consumed = true;
        if (isNearExpiry) {
            this.nearExpiryWhenConsumed = true;
            this.everNearExpiry = true;
        }
    }
}
