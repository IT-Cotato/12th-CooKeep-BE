package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.common.exception.EntityNotFoundException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
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
    private static final String DEFAULT_IMAGE =
            "https://cookeep-images.s3.ap-northeast-2.amazonaws.com/ingredients/0a0cc3bd-1ade-46ed-897e-ba89ec09b7c0.png";

    @Transactional
    public UserIngredientCreateResponseDto create(Long userId, UserIngredientCreateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        Type type;
        String ingredientName;
        String imageUrl;
        Storage finalStorage;
        LocalDate finalExpirationDate;

        // type에 따라 분기 처리
        if (request.getType() == Type.DEFAULT) {
            // DefaultIngredient 조회
            DefaultIngredient defaultIngredient = defaultIngredientRepository
                    .findById(request.getReferenceId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

            ingredientName = defaultIngredient.getIngredient();
            imageUrl = defaultIngredient.getImageUrl();

            // storage: 사용자가 입력하지 않으면 기본값 사용
            finalStorage = request.getStorage() != null
                    ? request.getStorage()
                    : defaultIngredient.getDefaultStorage();

            // expirationDate: 사용자가 입력하지 않으면 자동 계산
            finalExpirationDate = request.getExpirationDate() != null
                    ? request.getExpirationDate()
                    : LocalDate.now().plusDays(defaultIngredient.getDefaultExpirationDays());

        } else {
            // CustomIngredient 조회 (해당 유저의 것만)
            CustomIngredient customIngredient = customIngredientRepository
                    .findByIdAndUserId(request.getReferenceId(), userId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

            ingredientName = customIngredient.getName();
            imageUrl = customIngredient.getImageUrl();

            // storage: 사용자가 입력하지 않으면 기본값 사용
            finalStorage = request.getStorage() != null
                    ? request.getStorage()
                    : customIngredient.getStorage();

            // expirationDate: 사용자가 입력하지 않으면 자동 계산
            finalExpirationDate = request.getExpirationDate() != null
                    ? request.getExpirationDate()
                    : LocalDate.now().plusDays(customIngredient.getExpirationDays());
        }

        // UserIngredient 생성
        UserIngredient userIngredient = UserIngredient.builder()
                .type(request.getType())
                .referenceId(request.getReferenceId())
                .quantity(request.getQuantity())
                .unit(request.getUnit())
                .storage(finalStorage)
                .expirationDate(finalExpirationDate)
                .memo(request.getMemo())
                .user(user)
                .build();

        UserIngredient saved = userIngredientRepository.save(userIngredient);

        return UserIngredientCreateResponseDto.of(saved, ingredientName, imageUrl);
    }
}
