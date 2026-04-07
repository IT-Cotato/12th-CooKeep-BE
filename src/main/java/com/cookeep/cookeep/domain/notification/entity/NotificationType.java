package com.cookeep.cookeep.domain.notification.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EXPIRATION(
            "유통기한임박",
            "유통기한임박",
            "오늘 유통기한이 만료되는 재료가 있어요! \n 지금 확인하고 요리해볼까요?",
            "/api/users/me/refrigerator/home"),

    //필요시수정(to.현정)
    PLANT_WILTING(
            "시듦",
            "시듦",
            "식물이 시들었어요!",
            "/api/users/me"),
    PLANT_GROWTH_STOP(
            "성장정지",
            "성장정지",
            "성장정지되었어요",
            "/api/users/me"
            );
    private final String displayName;
    private final String title;
    private final String body;
    private final String url;

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
