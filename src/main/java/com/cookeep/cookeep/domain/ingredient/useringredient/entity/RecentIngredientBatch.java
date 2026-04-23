package com.cookeep.cookeep.domain.ingredient.useringredient.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recent_ingredient_batch")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecentIngredientBatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 마지막 등록 배치의 UUID
    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "ingredient_ids", columnDefinition = "JSON")
    private String ingredientIdsJson;

    @Builder
    public RecentIngredientBatch(User user, String batchId, String ingredientIdsJson) {
        this.user = user;
        this.batchId = batchId;
        this.ingredientIdsJson = ingredientIdsJson;
    }

    public void update(String batchId, String ingredientIdsJson) {
        this.batchId = batchId;
        this.ingredientIdsJson = ingredientIdsJson;
    }

}
