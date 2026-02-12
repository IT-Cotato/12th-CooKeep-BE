package com.cookeep.cookeep.api.dto.response;

public record GetNoticeResponseDTO(
	Long noticeId,
	String title,
	String content
) {
}
