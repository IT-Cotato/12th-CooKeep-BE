package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.*;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIngredientUpdateService {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    // 보관 장소 업데이트
    @Transactional
    public UserIngredientDetailResponseDto updateStorage(
            Long userId,
            Long ingredientId,
            UpdateStorageRequestDto request) {

        // 재료 조회
        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // Storage enum 변환
        Storage newStorage = request.getStorage();
        if (newStorage == null) {
            throw new AppException(ErrorCode.INVALID_STORAGE_TYPE);
        }

        // 보관 장소 변경
        userIngredient.updateStorage(newStorage);

        // 변경된 상세 정보 반환
        return getIngredientDetail(userId, ingredientId);
    }

    // 유통기한 업데이트
    @Transactional
    public UserIngredientDetailResponseDto updateExpirationDate(
            Long userId,
            Long ingredientId,
            UpdateExpirationRequestDto request) {

        // 재료 조회
        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // 유통기한 변경 (leftDays 자동 재계산)
        userIngredient.updateExpirationDate(request.getExpirationDate());

        // 변경된 상세 정보 반환
        return getIngredientDetail(userId, ingredientId);
    }

    // 수량 업데이트
    @Transactional
    public UserIngredientDetailResponseDto updateQuantity(
            Long userId,
            Long ingredientId,
            UpdateQuantityRequestDto request) {

        // 재료 조회
        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // 수량 0 이하면 에러처리
        if (request.getQuantity() < 0) {
            throw new AppException(ErrorCode.INVALID_QUANTITY);
        }

        // 수량 변경
        userIngredient.updateQuantity(request.getQuantity());

        // 변경된 상세 정보 반환
        return getIngredientDetail(userId, ingredientId);
    }

    // 메모 업데이트
    @Transactional
    public UserIngredientDetailResponseDto updateMemo(
            Long userId,
            Long ingredientId,
            UpdateMemoRequestDto request) {

        // 재료 조회
        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // 빈 칸이면 삭제
        String memo = request.getMemo();

        if (memo != null) {
            memo = memo.trim();
            if (memo.isEmpty()) {
                memo = null;
            }
        }

        // 메모 길이 100자 제한
        if (memo != null && memo.length() > 100) {
            throw new AppException(ErrorCode.MEMO_TOO_LONG);
        }

        // 메모 변경 (빈 문자열이면 메모 삭제)
        userIngredient.updateMemo(request.getMemo());

        // 변경된 상세 정보 반환
        return getIngredientDetail(userId, ingredientId);
    }

    // 식재료 상세 정보 조회 (변경 후 반환용)
    private UserIngredientDetailResponseDto getIngredientDetail(Long userId, Long ingredientId) {
        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // 식재료 이름, 이미지 URL 및 AI 팁 조회
        String ingredientName;
        String imageUrl;
        String aiTip = null;

        if (userIngredient.getType() == Type.DEFAULT) {
            DefaultIngredient defaultIngredient = defaultIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .orElse(null);
            ingredientName = defaultIngredient != null ? defaultIngredient.getIngredient() : "Unknown";
            imageUrl = defaultIngredient != null ? defaultIngredient.getImageUrl() : "";
            aiTip = defaultIngredient != null ? defaultIngredient.getAiTip() : null;
        } else {
            CustomIngredient customIngredient = customIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .orElse(null);
            ingredientName = customIngredient != null ? customIngredient.getName() : "Unknown";
            imageUrl = customIngredient != null ? customIngredient.getImageUrl() : "";
        }

        return UserIngredientDetailResponseDto.builder()
                .ingredientId(userIngredient.getIngredientId())
                .name(ingredientName)
                .imageUrl(imageUrl)
                .storage(userIngredient.getStorage().name())
                .expirationDate(userIngredient.getExpirationDate())
                .quantity(userIngredient.getQuantity())
                .leftDays(userIngredient.getLeftDays())
                .memo(userIngredient.getMemo())
                .aiTip(aiTip)
                .build();
    }

    @Transactional
    public void deleteUserIngredients(
            Long userId,
            DeleteUserIngredientsRequestDto request
    ) {
        List<Long> ids = request.getUserIngredientIds();

        // 1. 요청 검증
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_UPDATE_REQUEST);
        }

        // 2. 사용자 소유 재료 조회
        List<UserIngredient> userIngredients =
                userIngredientRepository.findAllByIdInAndUserId(ids, userId);

        if (userIngredients.isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 일부만 존재하는 경우
        if (userIngredients.size() != ids.size()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 3. 삭제 (리워드 지급 X)
        userIngredientRepository.deleteAll(userIngredients);

    }
}
