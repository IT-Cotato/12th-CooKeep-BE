package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Schema(description = "유통기한 변경 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
public class UpdateExpirationResponseDto {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "유통기한이 변경되었습니다.")
    private String message;

    @Schema(description = "변경된 유통기한", example = "2025-12-03", type = "string", format = "date")
    private LocalDate expirationDate;
}
