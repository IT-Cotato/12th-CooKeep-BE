package com.cookeep.cookeep.domain.recipe.entity;

public enum MessageType {

    INITIAL_REQUEST("첫 레시피 요청"),
    RETRY_REQUEST("다른 레시피를 받을래요"),
    ADOPT_RECIPE("이 레시피대로 요리할래요");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
