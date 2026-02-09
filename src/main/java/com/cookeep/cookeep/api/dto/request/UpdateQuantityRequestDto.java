package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "수량 변경 요청 DTO")
@Getter
@NoArgsConstructor
public class UpdateQuantityRequestDto {

    @Schema(description = "변경할 수량 (1 이상)", example = "3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer quantity;
}
