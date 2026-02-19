package com.cookeep.cookeep.domain.ingredient.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Storage {
    FRIDGE("냉장"),
    FREEZER("냉동"),
    PANTRY("상온");

    private final String displayName;

    @JsonCreator
    public static Storage from(String value) {
        for (Storage storage : Storage.values()) {
            // Enum 이름으로 매핑
            if (storage.name().equalsIgnoreCase(value)) {
                return storage;
            }
            // displayName으로 매핑
            if (storage.displayName.equals(value)) {
                return storage;
            }
        }
        throw new IllegalArgumentException("Invalid storage value: " + value);
    }

    @JsonValue
    public String toJson() {
        return this.displayName;
    }
}
