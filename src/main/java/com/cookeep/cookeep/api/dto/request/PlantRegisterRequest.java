package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PlantRegisterRequest {
    @NotNull(message = "등록할 식물 ID는 필수입니다.")
    private long plantId;
}
