package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.MyPlantResponse;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.plant.application.UserPlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "내 식물", description = "사용자가 보유한 식물 관리 API")
@RestController
@RequestMapping("/api/my-plants")
@RequiredArgsConstructor
public class MyPlantController {

    private final UserPlantService userPlantService;
    private final Long userId = 2L; // TODO: 시큐리티 적용 후 인증 객체에서 추출

    @Operation(summary = "보유 식물 목록 조회", description = "내가 현재 키우거나 수확한 식물 리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public DataResponse<List<MyPlantResponse>> getMyPlants() {
        return DataResponse.from(userPlantService.getMyPlants(userId));
    }

    @Operation(summary = "새로운 식물 등록", description = "기본 식물 ID를 경로에 전달하여 새로운 식물을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "식물 등록 성공"),
            @ApiResponse(responseCode = "404", description = "유저 또는 식물을 찾을 수 없음")
    })
    @PostMapping("/{plantId}") // /api/my-plants/{plantId}
    public DataResponse<Void> registerPlant(
            @Parameter(description = "기본 식물 ID") @PathVariable int plantId) {
        userPlantService.registerPlant(userId, plantId);
        return DataResponse.from(null);
    }

    @Operation(summary = "프로필 식물 지정", description = "내 보유 식물 ID를 경로에 전달하여 대표 프로필로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지정 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "유저 또는 식물을 찾을 수 없음")
    })
    @PatchMapping("/{userPlantId}/profile") // /api/my-plants/{userPlantId}/profile
    public DataResponse<Void> updateProfilePlant(
            @Parameter(description = "유저 보유 식물 ID (user_plant_id)") @PathVariable long userPlantId) {
        userPlantService.updateProfilePlant(userId, userPlantId);
        return DataResponse.from(null);
    }

    @Operation(summary = "식물 키우기 포기", description = "성장이 정지된 식물을 포기하고 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "포기 및 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "성장정지 상태가 아닌 식물은 포기할 수 없음"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "보유 식물을 찾을 수 없음")
    })
    @DeleteMapping("/{userPlantId}")
    public DataResponse<Void> abandonPlant(
            @Parameter(description = "포기할 보유 식물 ID") @PathVariable Long userPlantId) {
        userPlantService.abandonPlant(userId, userPlantId);
        return DataResponse.from(null);
    }

    @Operation(summary = "식물 물 주기", description = "보유한 식물에게 물을 줍니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "물 주기 성공"),
            @ApiResponse(responseCode = "400", description = "이미 수확했거나 얼어있는 상태"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "식물을 찾을 수 없음")
    })
    @PostMapping("/{userPlantId}/water")
    public DataResponse<Void> giveWater(@PathVariable Long userPlantId) {
        userPlantService.giveWater(userId, userPlantId);
        return DataResponse.from(null);
    }

    @Operation(summary = "식물 다시 살리기", description = "성장이 정지된(isFrozen=true) 식물을 쿠키를 사용하여 다시 살립니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "식물 살리기 성공"),
            @ApiResponse(responseCode = "400", description = "성장 정지 상태가 아님 (살릴 필요 없음)"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "보유 식물을 찾을 수 없음")
    })
    @PostMapping("/{userPlantId}/revive")
    public DataResponse<Void> revivePlant(
            @Parameter(description = "다시 살릴 보유 식물 ID") @PathVariable Long userPlantId) {
        userPlantService.revivePlant(userId, userPlantId);
        return DataResponse.from(null);
    }
}
