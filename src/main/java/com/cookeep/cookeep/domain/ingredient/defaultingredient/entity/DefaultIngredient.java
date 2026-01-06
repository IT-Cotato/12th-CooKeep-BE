package com.cookeep.cookeep.domain.ingredient.defaultingredient.entity;

import com.cookeep.cookeep.domain.ingredient.common.Category;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.common.Unit;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "default_ingredients")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DefaultIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "default_ingredient_id")
    private Long id;

    @Column(name = "default_ingredient", length = 100)
    private String ingredient;

    @Column(name = "default_expiration_days")
    private Integer defaultExpirationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_storage")
    private Storage defaultStorage;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_unit")
    private Unit unit;
}
