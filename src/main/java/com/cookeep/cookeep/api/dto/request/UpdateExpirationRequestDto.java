package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "유통기한 변경 요청 DTO")
@Getter
@NoArgsConstructor
public class UpdateExpirationRequestDto {

    @Schema(
            description = "변경할 유통기한 (YYYY-MM-DD)",
            example = "2025-12-03",
            type = "string",
            format = "date",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "유통기한은 필수입니다.")
    private LocalDate expirationDate;
}
