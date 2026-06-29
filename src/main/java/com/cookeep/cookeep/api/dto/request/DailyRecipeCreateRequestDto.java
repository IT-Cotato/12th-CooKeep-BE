package com.cookeep.cookeep.api.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(
        name = "DailyRecipeCreateRequest",
        description = "데일리 레시피 등록 요청 DTO"
)
@Getter
@NoArgsConstructor
public class DailyRecipeCreateRequestDto {

    @Schema(
            description = "채택된 AI 레시피 ID",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "AI 레시피 ID는 필수입니다.")
    private Long aiRecipeId;

    @Schema(
            description = "레시피 제목 (미입력 시 AI 레시피 제목 사용)",
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
            description = "요리 사진 URL (이미지 업로드 API로 먼저 업로드 후 URL 전달)",
            example = "https://cookeep-images.s3.amazonaws.com/recipe-images/abc.jpg",
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
            description = "공개 여부 (true: 쿠킵스에 공개, false: 나만 보기)",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "공개 여부는 필수입니다.")
    private Boolean isPublic;
}
