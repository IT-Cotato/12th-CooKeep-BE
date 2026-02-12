package com.cookeep.cookeep.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Schema(
        name = "ConsumptionReportResponse",
        description = "주간 식재료 소비 달성 현황 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class ConsumptionReportResponseDto {

    @Schema(description = "전체 식재료 수 (왼쪽 그래프 분모)", example = "20")
    private int totalIngredientCount;

    @Schema(description = "소비된 식재료 수 (왼쪽 그래프 분자)", example = "13")
    private int consumedIngredientCount;

    @Schema(description = "전체 소비율 (%, 0~100)", example = "65")
    private int consumptionRate;

    @Schema(description = "폐기 임박 식재료 수 (오른쪽 그래프 분모)", example = "5")
    private int nearExpiryIngredientCount;

    @Schema(description = "소비된 임박 식재료 수 (오른쪽 그래프 분자)", example = "5")
    private int consumedNearExpiryCount;

    @Schema(description = "임박 식재료 소비율 (%, 0~100)", example = "100")
    private int nearExpiryConsumptionRate;
}
