package com.cookeep.cookeep.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageFolder {
	PLANTS("plants"),
	RECIPE_IMAGES("recipeImages"),
	INGREDIENTS("ingredients");

	private final String folderName;

	public static ImageFolder fromValue(String value) {
		for (ImageFolder folder : ImageFolder.values()) {
			if (folder.folderName.equals(value)) {
				return folder;
			}
		}
		throw new IllegalArgumentException("Invalid ImageFolder value: " + value);
	}
}
