package com.cookeep.cookeep.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Value("${aws.s3.region}")
	private String region;

	public String upload(MultipartFile file, String folder) {
		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);

		validateFile(file, extension, folder);

		String key = folder + "/" + UUID.randomUUID() + "." + extension;
		String contentType = resolveContentType(file, extension);

		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(contentType)
				.build();

			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
		} catch (Exception e) {
			log.error("S3 upload failed. bucket={}, folder={}", bucket, folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
	}

	public String uploadCropped(MultipartFile file, String folder, int x, int y, int width, int height) {
		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);
		String outputFormat = "svg".equalsIgnoreCase(extension) ? "jpg" : extension;

		String key = folder + "/" + UUID.randomUUID() + "-cropped." + outputFormat;

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Thumbnails.of(file.getInputStream())
				.sourceRegion(x, y, width, height)
				.size(width, height)
				.keepAspectRatio(false)
				.outputFormat(outputFormat)
				.toOutputStream(baos);

			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType("image/" + outputFormat)
				.build();

			s3Client.putObject(request, RequestBody.fromBytes(baos.toByteArray()));
		} catch (IOException e) {
			log.error("이미지 크롭 실패. folder={}", folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		} catch (Exception e) {
			log.error("S3 크롭 이미지 업로드 실패. bucket={}, folder={}", bucket, folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
	}

	public void delete(String imageUrl) {
		String key = extractKeyFromUrl(imageUrl);

		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();

			s3Client.deleteObject(request);
		} catch (Exception e) {
			log.error("S3 delete failed. bucket={}, key={}", bucket, key, e);
			throw new AppException(ErrorCode.FILE_DELETE_ERROR);
		}
	}

	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "jpg";
		}
		return filename.substring(filename.lastIndexOf(".") + 1);
	}

	private String extractKeyFromUrl(String url) {
		String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
		return url.replace(prefix, "");
	}

	// --- svg용 ---
	// SVG 허용 폴더: INGREDIENTS (아이콘 전용)
	private static final Set<String> SVG_ALLOWED_FOLDERS = Set.of(
			ImageFolder.INGREDIENTS.getFolderName()
	);

	private void validateSvgContent(MultipartFile file) {
		try {
			String svg = new String(file.getBytes(), StandardCharsets.UTF_8).toLowerCase();

			if (svg.contains("<script")
					|| svg.contains("onload=")
					|| svg.contains("onclick=")
					|| svg.contains("<foreignobject")) {
				throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
			}
		} catch (Exception e) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private String resolveContentType(MultipartFile file, String extension) {
		if ("svg".equalsIgnoreCase(extension)) {
			return "image/svg+xml";
		}
		return file.getContentType();
	}

	private void validateFile(MultipartFile file, String extension, String folder) {
		if ("svg".equalsIgnoreCase(extension)) {
			validateSvgFolder(folder);
			validateSvgContentType(file);
			validateSvgContent(file);
		}
	}

	private void validateSvgFolder(String folder) {
		if (!SVG_ALLOWED_FOLDERS.contains(folder)) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private void validateSvgContentType(MultipartFile file) {
		if (!"image/svg+xml".equalsIgnoreCase(file.getContentType())) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

}
