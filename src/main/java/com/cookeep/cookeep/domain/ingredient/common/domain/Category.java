package com.cookeep.cookeep.domain.ingredient.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Category {

    VEGETABLE("채소"),
    FRUIT("과일"),
    MEAT("육류"),
    SEAFOOD("해산물"),
    DAIRY_EGG("유제품/계란"),
    GRAIN_RICE_NOODLE("곡물/쌀/면"),
    BAKERY("빵/베이커리"),
    SEASONING_SAUCE("양념/소스/조미료"),
    READY_MEAL("즉석/간편식"),
    SNACK_DESSERT("과자/스낵/디저트"),
    BEVERAGE("음료"),
    FERMENTED("절임/발효"),
    BEAN("콩류"),
    ETC("기타");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Category from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        // 별칭 매핑
        if (normalized.equals("BEANS")) {
            normalized = "BEAN";
        }
        try {
            return Category.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 카테고리 값: " + value);
        }
    }

}
