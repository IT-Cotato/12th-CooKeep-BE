package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "DailyRecipeUpdateRequest",
        description = "데일리 레시피 수정 요청 DTO"
)
@Getter
@NoArgsConstructor
public class DailyRecipeUpdateRequestDto {

    @Schema(
            description = "레시피 제목",
            example = "고추장 마요 달걀밥",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String title;

    @Schema(
            description = "한줄평",
            example = "버터가 다 녹고 프라이를 올리는 게 포인트",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String description;

    @Schema(
            description = "요리 사진 URL (이미지 업로드 API로 먼저 업로드 후 전달)",
            example = "https://cdn.cookeep.com/images/abc123.jpg",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String recipeImageUrl;
}
