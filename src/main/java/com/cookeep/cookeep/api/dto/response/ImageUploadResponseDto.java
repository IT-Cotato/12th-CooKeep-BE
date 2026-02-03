package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadResponseDto {

	private String imageUrl;

	public static ImageUploadResponseDto from(String imageUrl) {
		return ImageUploadResponseDto.builder()
			.imageUrl(imageUrl)
			.build();
	}
}
