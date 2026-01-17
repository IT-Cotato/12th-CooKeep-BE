package com.cookeep.cookeep.domain.ingredient.customingredient.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.api.dto.request.CustomIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.CustomIngredientCreateResponseDto;
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

        // 1. 유저 인증 확인
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 2. 필수 필드 존재 확인
        if (request == null
                || request.getName() == null || request.getName().isBlank()
                || request.getExpirationDays() == null
                || request.getStorage() == null
                || request.getCategory() == null) {

            throw new AppException(ErrorCode.CUSTOM_INGREDIENT_REQUIRED_FIELDS_MISSING);
        }

        // 3. ENUM 유효성 체크
        try {
            request.getStorage().name();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STORAGE_TYPE);
        }
        try {
            request.getCategory().name();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_CATEGORY_TYPE);
        }

        // 4. 중복 체크
        if (repository.existsByUserIdAndName(userId, request.getName())) {
            throw new AppException(ErrorCode.DUPLICATE_CUSTOM_INGREDIENT);
        }

        // 엔티티 생성
        CustomIngredient ingredient = new CustomIngredient(
                request.getName(),
                request.getExpirationDays(),
                request.getStorage(),
                request.getCategory(),
                userId
        );

        CustomIngredient saved = repository.save(ingredient);

        return CustomIngredientCreateResponseDto.from(saved);

    }
}
