package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.*;
import com.cookeep.cookeep.api.dto.response.ConsumeIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.EntityNotFoundException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.dao.CookieLogRepository;
import com.cookeep.cookeep.domain.cookie.dao.DailyCookieGrantRepository;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.cookie.entity.DailyCookieGrant;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIngredientUpdateService {

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final UserRepository userRepository;
    private final CookieLogRepository cookieLogRepository;
    private final DailyCookieGrantRepository dailyCookieGrantRepository;

    // 유통기한 임박 기준
    private static final int URGENT = 3;

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

    // 유저 식재료 삭제
    @Transactional
    public void deleteUserIngredients(
            Long userId,
            DeleteUserIngredientsRequestDto request
    ) {
        List<Long> ids = request.getUserIngredientIds();

        // 1. 요청 검증
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_DELETE_REQUEST);
        }

        // 2. 사용자 소유 재료 조회
        List<UserIngredient> userIngredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(ids, userId);

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

    // 식재료 섭취 완료
    @Transactional
    public ConsumeIngredientsResponseDto consumeIngredients(
            Long userId,
            ConsumeIngredientsRequestDto request
    ) {
        List<Long> ids = request.getUserIngredientIds();

        // 1. 요청 검증
        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_DELETE_REQUEST);
        }

        // 2. 사용자 소유 재료 조회
        List<UserIngredient> userIngredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(ids, userId);

        if (userIngredients.isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 일부만 존재하는 경우
        if (userIngredients.size() != ids.size()) {
            throw new AppException(ErrorCode.INGREDIENT_NOT_FOUND);
        }

        // 3. 리워드 처리
        RewardResult rewardResult = processRewards(userId, userIngredients);

        // 4. 재료 삭제
        userIngredientRepository.deleteAll(userIngredients);

        log.info("User {} consumed {} ingredients. Reward granted: {}, points: {}",
                userId, userIngredients.size(), rewardResult.isGranted(), rewardResult.getTotalPoints());

        return ConsumeIngredientsResponseDto.of(
                rewardResult.isGranted(),
                rewardResult.getTotalPoints(),
                rewardResult.getGrantedTypes()
        );
    }

    // --- 내부 메서드 ---

    private RewardResult processRewards(Long userId, List<UserIngredient> ingredients) {
        LocalDate today = LocalDate.now();

        int totalPoints = 0;
        boolean granted = false;
        List<CookieLog.CookieLogType> grantedTypes = new ArrayList<>();

        // 기본 일일 소비 리워드 확인
        CookieLog.CookieLogType dailyType = CookieLog.CookieLogType.BASIC_DAILY_FIRST_CONSUME;
        boolean alreadyGrantedDaily = dailyCookieGrantRepository.existsByUser_UserIdAndGrantTypeAndGrantDate(
                userId, dailyType, today
        );

        if (!alreadyGrantedDaily) {
            grantCookie(userId, dailyType, today);
            totalPoints += dailyType.getDefaultAmount();
            granted = true;
            grantedTypes.add(dailyType);
            log.info("Granted daily first consume reward to user {}: +{} cookies",
                    userId, dailyType.getDefaultAmount());
        }

        return new RewardResult(granted, totalPoints, grantedTypes);
    }

    private void grantCookie(Long userId, CookieLog.CookieLogType type, LocalDate grantDate) {
        // 1. 유저 조회 (비관적 락)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        int amount = type.getDefaultAmount();

        // 2. 쿠키 업데이트
        user.updateCookieCnt(amount);

        // 3. 쿠키 로그 저장
        CookieLog log = CookieLog.builder()
                .user(user)
                .amount(amount)
                .type(type)
                .build();
        cookieLogRepository.save(log);

        // 4. 일일 지급 기록 저장
        DailyCookieGrant grant = DailyCookieGrant.builder()
                .user(user)
                .grantType(type)
                .grantDate(grantDate)
                .build();
        dailyCookieGrantRepository.save(grant);
    }

    @lombok.Value
    private static class RewardResult {
        boolean granted;
        int totalPoints;
        List<CookieLog.CookieLogType> grantedTypes;
    }


}
