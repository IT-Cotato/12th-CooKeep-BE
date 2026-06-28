package com.cookeep.cookeep.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

@Component
public class SvgValidator {

	private static final Set<String> SVG_ALLOWED_FOLDERS = Set.of(
		ImageFolder.INGREDIENTS.getFolderName()
	);

	public void validate(MultipartFile file, String folder) {
		validateFolder(folder);
		validateContentType(file);
		validateContent(file);
	}

	private void validateFolder(String folder) {
		if (!SVG_ALLOWED_FOLDERS.contains(folder)) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private void validateContentType(MultipartFile file) {
		if (!"image/svg+xml".equalsIgnoreCase(file.getContentType())) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private void validateContent(MultipartFile file) {
		try {
			String svg = new String(file.getBytes(), StandardCharsets.UTF_8).toLowerCase();

			if (svg.contains("<script")
				|| svg.contains("onload=")
				|| svg.contains("onclick=")
				|| svg.contains("<foreignobject")) {
				throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
			}
		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}
}
