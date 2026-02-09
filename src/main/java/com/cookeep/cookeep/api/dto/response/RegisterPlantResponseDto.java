package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(
        name = "RegisterPlantResponse",
        description = "식물 등록 성공 응답 DTO"
)
public class RegisterPlantResponseDto {

    @Schema(description = "등록된 유저 식물 ID", example = "1")
    private Long userPlantId;

    @Schema(description = "응답 메시지", example = "첫 식물 등록이 완료되었습니다.")
    private String message;
}
