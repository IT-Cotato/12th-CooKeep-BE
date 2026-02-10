package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.RankingResponseDto;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.cookeeps.application.CookeepsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "쿠킵스", description = "쿠킵스(커뮤니티) 관련 API")
@RestController
@RequestMapping("/api/cookeeps")
@RequiredArgsConstructor
public class CookeepsController {

	private final CookeepsService cookeepsService;

	@Operation(summary = "이번 주 랭킹 조회", description = "이번 주 물주기 횟수 Top 3 유저와 좋아요 Top 3 레시피를 조회합니다.")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "조회 성공")
	})
	@GetMapping("/ranking")
	public DataResponse<RankingResponseDto> getRanking() {
		return DataResponse.from(cookeepsService.getRanking());
	}
}
