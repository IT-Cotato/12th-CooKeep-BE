package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "AiSessionTitleUpdateRequestDto",
        description = "AI 세션 제목 변경"
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiSessionTitleUpdateRequestDto {

    @Schema(
            description = "수정할 세션 제목",
            example = "김치볶음밥",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "제목은 비어 있을 수 없습니다.")
    @Size(max = 100, message = "제목은 최대 100자까지 가능합니다.")
    private String title;
}
