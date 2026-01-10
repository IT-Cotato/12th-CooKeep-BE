package com.cookeep.cookeep.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfilePlantRequest {
    @NotNull(message = "식물 ID는 필수입니다.") // null이 들어오면 에러 발생
    private Long userPlantId;
}
