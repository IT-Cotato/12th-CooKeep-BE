package com.cookeep.cookeep.domain.recipe.application;

// 랜덤 레시피 생성/검증 관련 공통 정책 상수

public final class RandomRecipePolicy {

    private RandomRecipePolicy() {
    }

    /** AI가 랜덤 레시피 생성 시 최소로 선택해야 하는 재료 개수 */
    public static final int RANDOM_MIN_SELECT_COUNT = 3;

    /** 선택 개수 부족 시 AiRecipeService에서 자동 재시도하는 최대 횟수 */
    public static final int RANDOM_SELECTION_MAX_ATTEMPTS = 3;
}
