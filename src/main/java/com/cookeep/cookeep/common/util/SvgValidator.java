package com.cookeep.cookeep.common.util;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

@Component
public class SvgValidator {

	private static final Set<String> SVG_ALLOWED_FOLDERS = Set.of(
		ImageFolder.INGREDIENTS.getFolderName()
	);

	// 안전한 SVG 태그·속성만 허용하는 화이트리스트 정책
	private static final PolicyFactory SVG_POLICY = new HtmlPolicyBuilder()
		.allowElements(
			"svg", "g", "path", "circle", "rect", "line", "polyline", "polygon",
			"ellipse", "text", "tspan", "defs", "use", "symbol", "clipPath",
			"linearGradient", "radialGradient", "stop", "mask", "pattern",
			"image", "title", "desc"
		)
		.allowAttributes(
			"xmlns", "viewBox", "width", "height", "x", "y", "x1", "y1", "x2", "y2",
			"cx", "cy", "r", "rx", "ry", "d", "fill", "stroke", "stroke-width",
			"stroke-linecap", "stroke-linejoin", "opacity", "transform",
			"clip-path", "mask", "id", "class", "style",
			"offset", "stop-color", "stop-opacity",
			"gradientUnits", "gradientTransform", "patternUnits",
			"font-size", "font-family", "text-anchor"
		).globally()
		.toFactory();

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
			String original = new String(file.getBytes(), StandardCharsets.UTF_8);
			String sanitized = SVG_POLICY.sanitize(original);

			if (!original.equals(sanitized)) {
				throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
			}
		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}
}
