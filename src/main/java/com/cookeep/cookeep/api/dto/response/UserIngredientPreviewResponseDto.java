package com.cookeep.cookeep.api.dto.response;

import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(
        name = "UserIngredientPreviewResponse",
        description = "(1) 식재료 기본 정보 조회 응답 DTO (단일 항목). DB에 저장된 기본값을 그대로 사용"
)
public class UserIngredientPreviewResponseDto {

    @Schema(description = "식재료 타입", example = "DEFAULT")
    private String type;

    @Schema(description = "참조 ID (요청에서 전달한 referenceId)", example = "3")
    private Long referenceId;

    @Schema(description = "식재료 이름", example = "상추")
    private String name;

    @Schema(description = "식재료 이미지 URL", example = "https://s3.amazonaws.com/cookeep/ingredients/lettuce.png")
    private String imageUrl;

    @Schema(description = "기본 수량 (항상 1)", example = "1")
    private Integer defaultQuantity;

    @Schema(description = "기본 단위 (DEFAULT=db값, CUSTOM=PIECE)", example = "PIECE")
    private String defaultUnit;

    @Schema(description = "기본 보관 장소", example = "FRIDGE")
    private String defaultStorage;

    @Schema(description = "기본 만료일 (오늘 + expirationDays, yyyy-MM-dd)", example = "2026-03-06", type = "string", format = "date")
    private LocalDate defaultExpirationDate;

    // ── 팩토리 메서드 ─────────────────────────────────────────────────────────

    public static UserIngredientPreviewResponseDto ofDefault(DefaultIngredient ref) {
        LocalDate expiration = calcExpiration(ref.getDefaultExpirationDays());

        return new UserIngredientPreviewResponseDto(
                "DEFAULT",
                ref.getId(),
                ref.getIngredient(),
                ref.getImageUrl(),
                1,
                ref.getUnit() != null ? ref.getUnit().name() : null,
                ref.getDefaultStorage() != null ? ref.getDefaultStorage().name() : null,
                expiration
        );
    }

    public static UserIngredientPreviewResponseDto ofCustom(CustomIngredient ref) {
        LocalDate expiration = calcExpiration(ref.getExpirationDays());

        return new UserIngredientPreviewResponseDto(
                "CUSTOM",
                ref.getId(),
                ref.getName(),
                ref.getImageUrl(),
                1,
                "PIECE",
                ref.getStorage() != null ? ref.getStorage().name() : null,
                expiration
        );
    }

    private static LocalDate calcExpiration(Integer expirationDays) {
        if (expirationDays == null) return null;
        return LocalDate.now().plusDays(expirationDays);
    }
}
