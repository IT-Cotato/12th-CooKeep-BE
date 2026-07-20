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

    @Schema(
            description = "크롭된 미리보기 사진 URL (이미지 업로드 API에서 cropX/Y/Width/Height 전달 시 반환되는 croppedImageUrl)",
            example = "https://cookeep-images.s3.amazonaws.com/recipeImages/abc-cropped.jpg",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String croppedImageUrl;

    @Schema(
            description = "요리 사진 삭제 여부 (true 시 기존 사진을 S3에서 삭제하고 DB에서 제거)",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private Boolean deleteRecipeImage;
}
