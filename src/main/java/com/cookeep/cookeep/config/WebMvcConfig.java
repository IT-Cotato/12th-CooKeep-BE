package com.cookeep.cookeep.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.cookeep.cookeep.common.util.ImageFolder;

@Component
public class WebMvcConfig implements WebMvcConfigurer {

	@Override
	public void addFormatters(FormatterRegistry registry) {
		registry.addConverter(new StringToImageFolderConverter());
	}

	public static class StringToImageFolderConverter implements Converter<String, ImageFolder> {
		@Override
		public ImageFolder convert(String source) {
			return ImageFolder.fromValue(source);
		}
	}
}
