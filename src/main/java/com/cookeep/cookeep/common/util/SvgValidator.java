package com.cookeep.cookeep.common.util;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;

@Component
public class SvgValidator {

	private static final Set<String> SVG_ALLOWED_FOLDERS = Set.of(
		ImageFolder.INGREDIENTS.getFolderName()
	);

	private static final Set<String> ALLOWED_ELEMENTS = Set.of(
		"svg", "g", "path", "circle", "rect", "line", "polyline", "polygon",
		"ellipse", "text", "tspan", "defs", "use", "symbol", "clippath",
		"lineargradient", "radialgradient", "stop", "mask", "pattern",
		"image", "title", "desc"
	);

	private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(
		"xmlns", "viewbox", "width", "height", "x", "y", "x1", "y1", "x2", "y2",
		"cx", "cy", "r", "rx", "ry", "d", "fill", "stroke", "stroke-width",
		"stroke-linecap", "stroke-linejoin", "opacity", "transform",
		"clip-path", "mask", "id", "class", "style",
		"offset", "stop-color", "stop-opacity",
		"gradientunits", "gradienttransform", "patternunits",
		"font-size", "font-family", "text-anchor",
		"href", "xlink:href"
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
			byte[] bytes = file.getBytes();
			String content = new String(bytes, StandardCharsets.UTF_8).toLowerCase();

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// XXE 방어
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

			Document doc = factory.newDocumentBuilder()
				.parse(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

			validateNode(doc.getDocumentElement());
		} catch (AppException e) {
			throw e;
		} catch (Exception e) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}
	}

	private void validateNode(Node node) {
		if (node.getNodeType() != Node.ELEMENT_NODE) {
			return;
		}

		String tagName = node.getLocalName() != null
			? node.getLocalName().toLowerCase()
			: node.getNodeName().toLowerCase();

		if (!ALLOWED_ELEMENTS.contains(tagName)) {
			throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
		}

		NamedNodeMap attrs = node.getAttributes();
		if (attrs != null) {
			for (int i = 0; i < attrs.getLength(); i++) {
				String attrName = attrs.item(i).getNodeName().toLowerCase();
				if (!ALLOWED_ATTRIBUTES.contains(attrName)) {
					throw new AppException(ErrorCode.FILE_UPLOAD_ERROR);
				}
			}
		}

		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			validateNode(children.item(i));
		}
	}
}
