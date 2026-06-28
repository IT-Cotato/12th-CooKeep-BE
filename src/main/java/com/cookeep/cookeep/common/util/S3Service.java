package com.cookeep.cookeep.common.util;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

	private final S3Uploader s3Uploader;
	private final ImageCropService imageCropService;
	private final SvgValidator svgValidator;

	public String upload(MultipartFile file, String folder) {
		String extension = extractExtension(file.getOriginalFilename());

		if ("svg".equalsIgnoreCase(extension)) {
			svgValidator.validate(file, folder);
			return uploadBytes(file, folder, extension, "image/svg+xml");
		}

		return uploadBytes(file, folder, extension, file.getContentType());
	}

	private String extractExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "jpg";
		}
		return filename.substring(filename.lastIndexOf(".") + 1);
	}

	private String uploadBytes(MultipartFile file, String folder, String extension, String contentType) {
		try {
			return s3Uploader.upload(file.getBytes(), folder, extension, contentType);
		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			log.error("파일 읽기 실패. folder={}", folder, e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	public String uploadCropped(MultipartFile file, String folder, int x, int y, int width, int height) {
		String extension = extractExtension(file.getOriginalFilename());
		String outputFormat = "svg".equalsIgnoreCase(extension) ? "jpg" : extension;

		byte[] cropped = imageCropService.crop(file, outputFormat, x, y, width, height);
		return s3Uploader.uploadCropped(cropped, folder, outputFormat, "image/" + outputFormat);
	}

	public void delete(String imageUrl) {
		s3Uploader.delete(imageUrl);
	}
}
