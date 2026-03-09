package com.cookeep.cookeep.domain.ingredient.useringredient.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Table(name = "user_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserIngredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ingredients_id")
    private Long ingredientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Unit unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Storage storage;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(columnDefinition = "TEXT", nullable = true)
    private String memo;

    @Column(name = "left_days", nullable = false)
    private Integer leftDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public UserIngredient(
            Type type,
            Long referenceId,
            Integer quantity,
            Unit unit, Storage storage,
            LocalDate expirationDate,
            String memo,
            User user) {

        this.type = type;
        this.referenceId = referenceId;
        this.quantity = quantity;
        this.unit = unit;
        this.storage = storage;
        this.expirationDate = expirationDate;
        this.memo = memo;
        this.user = user;
        this.leftDays = calculateLeftDays(expirationDate);
    }

    // 엔티티가 영속화되기 전에 자동으로 leftDays 계산
    @PrePersist
    public void prePersist() {
        this.leftDays = calculateLeftDays(this.expirationDate);
    }

    // 엔티티가 업데이트되기 전에 자동으로 leftDays 재계산
    @PreUpdate
    public void preUpdate() {
        this.leftDays = calculateLeftDays(this.expirationDate);
    }

    private Integer calculateLeftDays(LocalDate expirationDate) {
        return (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expirationDate);
    }

    // leftDays를 현재 날짜 기준으로 재계산
    public void updateLeftDays() {
        Integer newLeftDays = calculateLeftDays(this.expirationDate);
        // 값이 변경되었을 때만 업데이트 (최적화)
        if (!newLeftDays.equals(this.leftDays)) {
            this.leftDays = newLeftDays;
        }
    }

    // 조회 시점에 실시간으로 leftDays 반환
    public Integer getLeftDays() {
        return calculateLeftDays(this.expirationDate);
    }

    // 보관 장소 변경
    public void updateStorage(Storage storage) {
        this.storage = storage;
    }

    // 유통기한 변경 (leftDays도 자동 재계산)
    public void updateExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        this.leftDays = calculateLeftDays(expirationDate);
    }

    // 수량 변경
    public void updateQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // 메모 변경
    public void updateMemo(String memo) {
        this.memo = memo;
    }

}
