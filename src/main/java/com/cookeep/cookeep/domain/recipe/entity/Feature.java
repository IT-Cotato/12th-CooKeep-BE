package com.cookeep.cookeep.domain.recipe.entity;

public enum Feature {

    SOUP_STEW("국물/찌개"),
    RICE_BOWL("밥/덮밥"),
    NOODLE("면 요리"),
    STIR_FRY_GRILL("볶음/구이"),
    SALAD_HEALTHY("샐러드/건강식"),
    SNACK_DESSERT("간식/디저트"),
    ANY("아무거나");

    private final String displayName;

    Feature(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
