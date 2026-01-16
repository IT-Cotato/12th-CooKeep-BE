package com.cookeep.cookeep.domain.ingredient.customingredient.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.api.dto.CustomIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.CustomIngredientCreateResponseDto;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomIngredientService {

    private final CustomIngredientRepository repository;

    public CustomIngredientCreateResponseDto create(
            Long userId,
            CustomIngredientCreateRequestDto request) {

        // 1. 중복 체크
        if (repository.existsByUserIdAndName(userId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_CUSTOM_INGREDIENT);
        }

        // 2. 엔티티 생성
        CustomIngredient ingredient = new CustomIngredient(
                request.getName(),
                request.getExpirationDays(),
                request.getStorage(),
                request.getCategory(),
                userId
        );

        // 3. 저장
        CustomIngredient saved = repository.save(ingredient);

        // 4. 응답 반환
        return CustomIngredientCreateResponseDto.from(saved);

    }
}
