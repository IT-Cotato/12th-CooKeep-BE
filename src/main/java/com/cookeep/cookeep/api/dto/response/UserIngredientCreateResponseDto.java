package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(
        name = "UserIngredientCreateResponse",
        description = "유저 보유 식재료 등록 성공 응답 DTO"
)
public class UserIngredientCreateResponseDto {

    @Schema(description = "식재료 타입", example = "CUSTOM")
    private String type;

    @Schema(description = "유저 식재료(UserIngredient DB) ID", example = "1")
    private Long ingredientId;

    @Schema(description = "참조 ID (요청에서 전달한 referenceId)", example = "3")
    private Long referenceId;

    @Schema(description = "식재료 이름", example = "두쫀쿠")
    private String name;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "단위", example = "PIECE")
    private String unit;

    @Schema(description = "보관 장소", example = "FRIDGE")
    private String storage;

    @Schema(description = "만료일 (yyyy-MM-dd)", example = "2026-01-20", type = "string", format = "date")
    private LocalDate expirationDate;

    @Schema(description = "남은 일수(만료일까지)", example = "4")
    private Integer leftDays;

    @Schema(description = "메모", example = "유통기한 짧음. 먼저 사용")
    private String memo;

    @Schema(description = "식재료 이미지 URL", example = "https://s3.amazonaws.com/cookeep/ingredients/carrot.png")
    private String imageUrl;

    public static UserIngredientCreateResponseDto of(
            UserIngredient userIngredient,
            String ingredientName,
            String imageUrl
    ) {
        return new UserIngredientCreateResponseDto(
                userIngredient.getType().name(),
                userIngredient.getIngredientId(),
                userIngredient.getReferenceId(),
                ingredientName,
                userIngredient.getQuantity(),
                userIngredient.getUnit().name(),
                userIngredient.getStorage().name(),
                userIngredient.getExpirationDate(),
                userIngredient.getLeftDays(),
                userIngredient.getMemo(),
                imageUrl
        );
    }
}
