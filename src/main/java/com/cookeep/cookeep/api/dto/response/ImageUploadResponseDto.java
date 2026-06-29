package com.cookeep.cookeep.api.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageUploadResponseDto {

	private String imageUrl;

	private String croppedImageUrl;

	public static ImageUploadResponseDto from(String imageUrl) {
		return ImageUploadResponseDto.builder()
			.imageUrl(imageUrl)
			.build();
	}

	public static ImageUploadResponseDto of(String imageUrl, String croppedImageUrl) {
		return ImageUploadResponseDto.builder()
			.imageUrl(imageUrl)
			.croppedImageUrl(croppedImageUrl)
			.build();
	}
}
