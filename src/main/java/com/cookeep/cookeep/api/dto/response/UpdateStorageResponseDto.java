package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "보관 장소 변경 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
public class UpdateStorageResponseDto {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "보관 장소가 변경되었습니다.")
    private String message;

    @Schema(description = "변경된 보관 장소", example = "FREEZER")
    private String storage;
}
