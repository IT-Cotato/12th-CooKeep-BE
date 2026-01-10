package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.ProfilePlantRequest;
import com.cookeep.cookeep.api.dto.response.MyPlantResponse;
import com.cookeep.cookeep.api.dto.response.PlantResponse;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.Plants.application.PlantService;
import com.cookeep.cookeep.domain.Plants.application.UserPlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "식물", description = "식물 관련 API")
@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;
    private final UserPlantService userPlantService;

    @Operation(summary = "전체 식물 도감 목록 조회", description = "시스템에 등록된 모든 식물 종류를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public DataResponse<List<PlantResponse>> getAllPlants() {
        List<PlantResponse> responses = plantService.getAllPlants().stream()
                .map(PlantResponse::from)
                .collect(Collectors.toList());

        return DataResponse.from(responses);
    }

    @Operation(summary = "내 보유 식물 목록 조회", description = "내가 현재 키우거나 수확한 식물 리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/me")
    public DataResponse<List<MyPlantResponse>> getMyPlants() {
        // TODO: 시큐리티 적용 전이므로 임시 유저 ID 1L 사용
        Long userId = 1L;
        return DataResponse.from(userPlantService.getMyPlants(userId));
    }

    @Operation(summary = "프로필 식물 지정", description = "보유한 식물 중 하나를 대표 프로필로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지정 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "유저 또는 식물을 찾을 수 없음")
    })
    @PatchMapping("/me/profile-plant")
    public DataResponse<Void> updateProfilePlant(@Valid @RequestBody ProfilePlantRequest request) {
        // TODO: 시큐리티 적용 전 임시 ID
        Long userId = 1L;
        userPlantService.updateProfilePlant(userId, request.getUserPlantId());
        return DataResponse.from(null);
    }
}
