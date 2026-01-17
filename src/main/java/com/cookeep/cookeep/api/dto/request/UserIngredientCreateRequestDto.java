package com.cookeep.cookeep.api.dto.request;

import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.common.Unit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class UserIngredientCreateRequestDto {

    @NotNull(message = "식재료 타입은 필수입니다.")
    private Type type;

    @NotNull(message = "식재료 ID는 필수입니다.")
    private Long referenceId;

    @NotNull(message = "수량은 필수입니다.")
    @Positive(message = "수량은 양수여야 합니다.")
    private Integer quantity;

    @NotNull(message = "단위는 필수입니다.")
    private Unit unit;

    private Storage storage;

    private LocalDate expirationDate;

    private String memo;
}
