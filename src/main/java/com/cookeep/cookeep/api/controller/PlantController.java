package com.cookeep.cookeep.api.controller;

import com.cookeep.cookeep.api.dto.response.PlantResponse;
import com.cookeep.cookeep.common.dto.DataResponse;
import com.cookeep.cookeep.domain.plant.application.PlantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "기본 식물", description = "기본 식물 정보 API")
@RestController
@RequestMapping("/api/plants")
@RequiredArgsConstructor
public class PlantController {

    private final PlantService plantService;

    @Operation(summary = "전체 식물 목록 조회", description = "시스템에 등록된 모든 식물 종류를 조회합니다.")
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
}
