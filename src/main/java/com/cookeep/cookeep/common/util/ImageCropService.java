package com.cookeep.cookeep.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

@Slf4j
@Service
public class ImageCropService {

	public byte[] crop(MultipartFile file, String outputFormat, int x, int y, int width, int height) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Thumbnails.of(file.getInputStream())
				.sourceRegion(x, y, width, height)
				.size(width, height)
				.keepAspectRatio(false)
				.outputFormat(outputFormat)
				.toOutputStream(baos);
			return baos.toByteArray();
		} catch (IOException e) {
			log.error("이미지 크롭 실패.", e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}
}
