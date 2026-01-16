package com.cookeep.cookeep.domain.ingredient.common;

public enum Unit {

    PIECE("개"),
    PACK("팩"),
    BAG("봉지"),
    BOTTLE("병"),
    BUNDLE("묶음"),
    CAN("캔"),
    GRAM("g"),
    MILLILITER("ml");

    private final String displayName;

    Unit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
