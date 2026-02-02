package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Schema(
        name = "AiSessionDetailResponse",
        description = "AI 레시피 대화 상세 리스트 응답 DTO"
)
@Getter
@Builder
@AllArgsConstructor
public class AiSessionDetailResponseDto {

    @Schema(
            description = "세션 ID",
            example = "21",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long sessionId;

    @Schema(
            description = "대화 내역 목록",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<ConversationTurn> conversations;

    @Schema(
            name = "ConversationTurn",
            description = "대화 턴 정보"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class ConversationTurn {

        @Schema(
                description = "대화 턴 번호",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private Integer turn;

        @Schema(
                description = "AI가 생성한 레시피",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private RecipeInfo recipe;

        @Schema(
                description = "레시피 생성 시간",
                example = "2025-02-01T14:32:10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private LocalDateTime createdAt;
    }

    @Schema(
            name = "RecipeInfo",
            description = "레시피 정보"
    )
    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecipeInfo {

        @Schema(
                description = "레시피 제목",
                example = "양파 고구마순 볶음",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private String title;

        @Schema(
                description = "재료 정보",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private GeminiRecipeResponseDto.Ingredients ingredients;

        @Schema(
                description = "조리 단계",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        private List<String> steps;

        @Schema(
                description = "유튜브 참고 영상",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        private List<GeminiRecipeResponseDto.YoutubeReference> youtubeReferences;

        public static RecipeInfo from(GeminiRecipeResponseDto dto) {
            return RecipeInfo.builder()
                    .title(dto.getTitle())
                    .ingredients(dto.getIngredients())
                    .steps(dto.getSteps())
                    .youtubeReferences(dto.getYoutubeReferences())
                    .build();
        }
    }
}
