package com.cookeep.cookeep.domain.ingredient.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Storage {
    FRIDGE("냉장"),
    FREEZER("냉동"),
    PANTRY("상온");

    private final String displayName;
}
