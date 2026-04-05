package com.cookeep.cookeep.domain.dailyrecipe.entity;

import com.cookeep.cookeep.common.entity.BaseEntity;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.user.entity.User;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "daily_recipes", indexes = {
        @Index(name = "idx_daily_recipes_public_created", columnList = "is_public, created_at DESC"),
        @Index(name = "idx_daily_recipes_public_likes", columnList = "is_public, like_count DESC, created_at DESC")
})
public class DailyRecipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_recipe_id", nullable = false)
    private Long id;

    @Column(name = "title", length = 100, nullable = false)
    private String title; // (AiRecipe 기본값, 사용자 수정 가능)

    @Column(name = "description", length = 255)
    private String description; // 한줄평

    @Lob
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // AiRecipe에서 복사한 레시피 내용 스냅샷

    @Column(name = "recipe_image_url", length = 512)
    private String recipeImageUrl;

    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Builder.Default
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Builder.Default
    @Column(name = "is_photo_reward_completed", nullable = false)
    private boolean photoRewardCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_recipes_id")
    private AiRecipe aiRecipe; // 원본 AI 레시피 참조 (삭제 시 null)

    public void updateVisibility(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateRecipeImageUrl(String recipeImageUrl) {
        this.recipeImageUrl = recipeImageUrl;
    }

    public void markPhotoRewardCompleted() {
        this.photoRewardCompleted = true;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }
}
