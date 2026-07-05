package com.cookeep.cookeep.common.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

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
			BufferedImage image = ImageIO.read(file.getInputStream());
			validateCropBounds(x, y, width, height, image.getWidth(), image.getHeight());

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Thumbnails.of(image)
				.sourceRegion(x, y, width, height)
				.size(width, height)
				.keepAspectRatio(false)
				.outputFormat(outputFormat)
				.toOutputStream(baos);
			return baos.toByteArray();
		} catch (AppException e) {
			throw e;
		} catch (IOException e) {
			log.error("이미지 크롭 실패.", e);
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private void validateCropBounds(int x, int y, int width, int height, int imageWidth, int imageHeight) {
		if (x < 0 || y < 0 || width <= 0 || height <= 0
			|| x + width > imageWidth
			|| y + height > imageHeight) {
			throw new AppException(ErrorCode.FILE_CROP_INVALID_BOUNDS);
		}
	}
}
