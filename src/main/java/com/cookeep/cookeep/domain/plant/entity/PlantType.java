package com.cookeep.cookeep.domain.plant.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlantType {
    POTATO("감자"),
    TOMATO("토마토"),
    KIDNEY_BEAN("강낭콩"),
    LETTUCE("상추"),
    STRAWBERRY("딸기"),
    APPLE("사과");

    private final String displayName;
}
