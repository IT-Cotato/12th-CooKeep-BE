package com.cookeep.cookeep.domain.refrigerator.application;

import com.cookeep.cookeep.api.dto.response.RefrigeratorIngredientsResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefrigeratorService {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    // 냉장고 전체 식재료 조회 (보관 장소별 그룹화)
    public RefrigeratorIngredientsResponseDto getAllIngredients(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 각 보관 장소별로 조회 (유통기한 임박순 정렬)
        List<UserIngredient> fridgeIngredients = userIngredientRepository
                .findByUserIdAndStorage(userId, Storage.FRIDGE);
        List<UserIngredient> freezerIngredients = userIngredientRepository
                .findByUserIdAndStorage(userId, Storage.FREEZER);
        List<UserIngredient> pantryIngredients = userIngredientRepository
                .findByUserIdAndStorage(userId, Storage.PANTRY);

        return RefrigeratorIngredientsResponseDto.builder()
                .fridge(convertToIngredientItems(fridgeIngredients))
                .freezer(convertToIngredientItems(freezerIngredients))
                .pantry(convertToIngredientItems(pantryIngredients))
                .build();
    }

    // --- 내부 메서드 ---
    // UserIngredient 리스트를 IngredientItem 리스트로 변환
    private List<RefrigeratorIngredientsResponseDto.IngredientItem> convertToIngredientItems(
            List<UserIngredient> userIngredients) {

        return userIngredients.stream()
                .map(ui -> {
                    String name;
                    String imageUrl;

                    if (ui.getType() == Type.DEFAULT) {
                        DefaultIngredient defaultIngredient = defaultIngredientRepository
                                .findById(ui.getReferenceId())
                                .orElse(null);
                        name = defaultIngredient != null ? defaultIngredient.getIngredient() : "Unknown";
                        imageUrl = defaultIngredient != null ? defaultIngredient.getImageUrl() : "";
                    } else {
                        CustomIngredient customIngredient = customIngredientRepository
                                .findById(ui.getReferenceId())
                                .orElse(null);
                        name = customIngredient != null ? customIngredient.getName() : "Unknown";
                        imageUrl = customIngredient != null ? customIngredient.getImageUrl() : "";
                    }

                    return RefrigeratorIngredientsResponseDto.IngredientItem.builder()
                            .type(ui.getType().name())
                            .referenceId(ui.getReferenceId())
                            .name(name)
                            .leftDays(ui.getLeftDays())
                            .imageUrl(imageUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
