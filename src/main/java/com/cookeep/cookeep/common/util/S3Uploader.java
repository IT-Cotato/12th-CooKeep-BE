package com.cookeep.cookeep.common.util;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucket;

	@Value("${aws.s3.region}")
	private String region;

	public String upload(byte[] data, String folder, String extension, String contentType) {
		String key = folder + "/" + UUID.randomUUID() + "." + extension;

		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(contentType)
				.build();

			s3Client.putObject(request, RequestBody.fromBytes(data));
		} catch (Exception e) {
			log.error("S3 upload failed. bucket={}, folder={}", bucket, folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		return buildUrl(key);
	}

	public String uploadCropped(byte[] data, String folder, String extension, String contentType) {
		String key = folder + "/" + UUID.randomUUID() + "-cropped." + extension;

		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(contentType)
				.build();

			s3Client.putObject(request, RequestBody.fromBytes(data));
		} catch (Exception e) {
			log.error("S3 cropped image upload failed. bucket={}, folder={}", bucket, folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		return buildUrl(key);
	}

	public void delete(String imageUrl) {
		String key = extractKey(imageUrl);

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

	public String buildUrl(String key) {
		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
	}

	private String extractKey(String url) {
		String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, region);
		return url.replace(prefix, "");
	}
}
