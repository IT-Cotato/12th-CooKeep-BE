package com.cookeep.cookeep.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageFolder {
	PLANTS("plants"),
	RECIPE_IMAGES("recipeImages");

	private final String folderName;
}
