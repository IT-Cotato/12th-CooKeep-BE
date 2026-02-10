package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "보관 장소 변경 요청 DTO")
@Getter
@NoArgsConstructor
public class UpdateStorageRequestDto {

    @Schema(description = "변경할 보관 장소", example = "FREEZER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "보관 장소는 필수입니다.")
    private Storage storage;
}
