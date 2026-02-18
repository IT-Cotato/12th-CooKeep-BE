package com.cookeep.cookeep.domain.ingredient.customingredient.entity;

import com.cookeep.cookeep.domain.ingredient.common.domain.Category;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custom_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_ingredient_id")
    private Long id;

    @Column(name = "custom_ingredient", nullable = false, length = 100)
    private String name;

    @Column(name = "custom_expiration_days", nullable = false)
    private Integer expirationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "custom_storage", nullable = false)
    private Storage storage;

    @Enumerated(EnumType.STRING)
    @Column(name = "custom_category", nullable = false)
    private Category category;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    public CustomIngredient(
            String name,
            Integer expirationDays,
            Storage storage,
            Category category,
            Long userId
    ) {
        this.name = name;
        this.expirationDays = expirationDays;
        this.storage = storage;
        this.category = category;
        this.userId = userId;
        this.imageUrl = "https://cookeep-images.s3.ap-northeast-2.amazonaws.com/ingredients/default.png";
    }
}
