package com.cookeep.cookeep.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.api.dto.response.ImageUploadResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.common.util.S3Service;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/images")
@Tag(name = "이미지", description = "이미지 업로드/삭제 API")
public class ImageController {

	private final S3Service s3Service;

	@Operation(summary = "이미지 업로드", description = "S3에 이미지를 업로드하고 URL을 반환합니다.")
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<DataResponse<ImageUploadResponseDto>> uploadImage(
		@RequestPart("image") MultipartFile image,
		@RequestParam(value = "folder", defaultValue = "images") String folder
	) {
		String imageUrl = s3Service.upload(image, folder);
		return ResponseEntity.ok(DataResponse.from(ImageUploadResponseDto.from(imageUrl)));
	}

	@Operation(summary = "이미지 삭제", description = "S3에서 이미지를 삭제합니다.")
	@DeleteMapping
	public ResponseEntity<DataResponse<Void>> deleteImage(
		@RequestParam("imageUrl") String imageUrl
	) {
		s3Service.delete(imageUrl);
		return ResponseEntity.ok(DataResponse.ok());
	}
}
