package com.cookeep.cookeep.common.util;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
		String key = folder + "/" + UUID.randomUUID() + "." + extension;

		try {
			PutObjectRequest request = PutObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.contentType(file.getContentType())
				.build();

			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
		} catch (IOException e) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
	}

	public void delete(String imageUrl) {
		String key = extractKeyFromUrl(imageUrl);

		DeleteObjectRequest request = DeleteObjectRequest.builder()
			.bucket(bucket)
			.key(key)
			.build();

		s3Client.deleteObject(request);
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
}
