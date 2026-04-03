package com.cookeep.cookeep.domain.notification.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EXPIRATION("유통기한임박"),
    PLANT_WILTING("시듦"), //필요시수정(to.현정)
    PLANT_GROWTH_STOP("성장정지"); //필요시수정(to.현정)

    private final String displayName;

    @JsonCreator
    public static NotificationType from(String value) {
        for (NotificationType type : NotificationType.values()) {

            // enum 이름으로 매핑 (EXPIRATION 등)
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }

            // displayName으로 매핑 (유통기한임박 등)
            if (type.displayName.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid notification type: " + value);
    }

    @JsonValue
    public String toJson() {
        return this.displayName;
    }
}
