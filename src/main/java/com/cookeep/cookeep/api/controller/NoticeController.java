package com.cookeep.cookeep.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cookeep.cookeep.api.dto.response.GetNoticeResponseDTO;
import com.cookeep.cookeep.api.dto.response.UserProfileResponseDTO;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.notice.application.NoticeService;
import com.cookeep.cookeep.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
public class NoticeController {
	private final NoticeService noticeService;

	// 공지사항 조회
	@Operation(summary = "공지사항 조회 API")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "요청 성공"),
		@ApiResponse(responseCode = "401", description = "회원 인증 실패 (AccessToken이 없거나 유효하지 않음)")
	})
	@GetMapping()
	public ResponseEntity<DataResponse<List<GetNoticeResponseDTO>>> getNotices() {
		return ResponseEntity.ok(DataResponse.from(noticeService.getNotices()));
	}
}
