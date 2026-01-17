package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.common.exception.EntityNotFoundException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.Storage;
import com.cookeep.cookeep.domain.ingredient.common.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIngredientService {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final UserRepository userRepository;

    @Transactional
    public UserIngredientCreateResponseDto create(Long userId, UserIngredientCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // DefaultIngredient에서 조회
        DefaultIngredient defaultIngredient = defaultIngredientRepository
                .findById(request.getReferenceId())
                .orElse(null);

        Type type;
        String ingredientName;
        Storage finalStorage;
        LocalDate finalExpirationDate;

        // 1. DefaultIngredient인 경우
        if (defaultIngredient != null) {
            // DefaultIngredient로 등록
            type = Type.DEFAULT;
            ingredientName = defaultIngredient.getIngredient();

            // storage: 사용자가 입력하지 않으면 DB에 저장된 값으로
            finalStorage = request.getStorage() != null
                    ? request.getStorage()
                    : defaultIngredient.getDefaultStorage();

            // expirationDate: 사용자가 입력하지 않으면 DB에 저장된 값으로 계산
            finalExpirationDate = request.getExpirationDate() != null
                    ? request.getExpirationDate()
                    : LocalDate.now().plusDays(defaultIngredient.getDefaultExpirationDays());

        }
        // 2. CustomIngredient인 경우
        else {
            // CustomIngredient에서 찾기
            CustomIngredient customIngredient = customIngredientRepository
                    .findByIdAndUserId(request.getReferenceId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

            type = Type.CUSTOM;
            ingredientName = customIngredient.getName();

            // storage: 사용자가 입력하지 않으면 DB에 저장된 값으로
            finalStorage = request.getStorage() != null
                    ? request.getStorage()
                    : customIngredient.getStorage();

            // expirationDate: 사용자가 입력하지 않으면 DB에 저장된 값으로 계산
            finalExpirationDate = request.getExpirationDate() != null
                    ? request.getExpirationDate()
                    : LocalDate.now().plusDays(customIngredient.getExpirationDays());
        }

        // UserIngredient 생성
        UserIngredient userIngredient = UserIngredient.builder()
                .type(type)
                .referenceId(request.getReferenceId())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .storage(finalStorage)
                .expirationDate(finalExpirationDate)
                .memo(request.getMemo())
                .user(user)
                .build();

        UserIngredient saved = userIngredientRepository.save(userIngredient);

        return UserIngredientCreateResponseDto.of(saved, ingredientName);
    }
}
