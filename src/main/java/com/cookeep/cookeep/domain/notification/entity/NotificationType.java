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
            "/fridge"),

    PLANT_WILTING(
            "시듦",
            "식물이 시들기 시작했어요",
            "오늘 한 끼만 챙겨주면 다시 살아나요🌱",
            "/cookeeps"),
    PLANT_GROWTH_STOP(
            "성장정지",
            "🛑식물 성장이 멈췄어요",
            "지금 돌아오면 다시 키울 수 있어요!",
            "/cookeeps");
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
