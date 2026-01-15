package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.request.PlantRegisterRequest;
import com.cookeep.cookeep.api.dto.request.ProfilePlantRequest;
import com.cookeep.cookeep.api.dto.response.MyPlantResponse;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.plant.application.UserPlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "내 식물", description = "사용자가 보유한 식물 관리 API")
@RestController
@RequestMapping("/api/my-plants")
@RequiredArgsConstructor
public class MyPlantController {

    private final UserPlantService userPlantService;
    private final Long userId = 1L; // TODO: 시큐리티 적용 후 인증 객체에서 추출

    @Operation(summary = "보유 식물 목록 조회", description = "내가 현재 키우거나 수확한 식물 리스트를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public DataResponse<List<MyPlantResponse>> getMyPlants() {
        return DataResponse.from(userPlantService.getMyPlants(userId));
    }

    @Operation(summary = "새로운 식물 등록(심기)", description = "기본 식물 중 하나를 선택하여 현재 키우는 식물로 새롭게 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "식물 등록 성공"),
            @ApiResponse(responseCode = "404", description = "유저 또는 도감 식물을 찾을 수 없음")
    })
    @PostMapping
    public DataResponse<Void> registerPlant(@Valid @RequestBody PlantRegisterRequest request) {
        userPlantService.registerPlant(userId, request.getPlantId());
        return DataResponse.from(null);
    }

    @Operation(summary = "프로필 식물 지정", description = "내 식물 중 하나를 대표 프로필로 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "지정 성공"),
            @ApiResponse(responseCode = "403", description = "본인의 식물이 아님"),
            @ApiResponse(responseCode = "404", description = "식물을 찾을 수 없음")
    })
    @PatchMapping("/profile")
    public DataResponse<Void> updateProfilePlant(@Valid @RequestBody ProfilePlantRequest request) {
        userPlantService.updateProfilePlant(userId, request.getUserPlantId());
        return DataResponse.from(null);
    }
}
