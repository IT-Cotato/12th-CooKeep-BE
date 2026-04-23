package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.request.UserIngredientCreateRequestDto;
import com.cookeep.cookeep.api.dto.request.UserIngredientPreviewRequestDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListCreateResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientListPreviewResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientPreviewResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.cookie.application.CookieService;
import com.cookeep.cookeep.domain.cookie.entity.CookieLog;
import com.cookeep.cookeep.domain.ingredient.common.domain.Storage;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.common.domain.Unit;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.mycookeep.application.ConsumptionReportService;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserIngredientServiceImpl implements UserIngredientService {

    private static final int DEFAULT_QUANTITY = 1;
    private static final Unit DEFAULT_CUSTOM_UNIT = Unit.PIECE;

    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final ConsumptionReportService consumptionReportService;
    private final UserRepository userRepository;
    private final RecentIngredientService recentIngredientService;
    private final CookieService cookieService;

    // 1. 기본 정보 조회 (저장 안 함)
    @Override
    @Transactional(readOnly = true)
    public UserIngredientListPreviewResponseDto previewAll(Long userId, List<UserIngredientPreviewRequestDto> requests) {
        List<UserIngredientPreviewResponseDto> results = requests.stream()
                .map(req -> previewOne(userId, req))
                .toList();

        return UserIngredientListPreviewResponseDto.of(results);
    }

    private UserIngredientPreviewResponseDto previewOne(Long userId, UserIngredientPreviewRequestDto req) {
        if (req.getType() == Type.DEFAULT) {
            DefaultIngredient ref = defaultIngredientRepository.findById(req.getReferenceId())
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));
            return UserIngredientPreviewResponseDto.ofDefault(ref);

        } else if (req.getType() == Type.CUSTOM) {
            CustomIngredient ref = customIngredientRepository
                    .findByIdAndUserId(req.getReferenceId(), userId)
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));
            return UserIngredientPreviewResponseDto.ofCustom(ref);

        } else {
            throw new AppException(ErrorCode.INVALID_INGREDIENT_REQUEST);
        }
    }

    // 2. 최종등록
    @Override
    public UserIngredientListCreateResponseDto createAll(Long userId, List<UserIngredientCreateRequestDto> requests) {
        // 유저 존재 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 등록 배치 UUID 생성
        String batchId = RecentIngredientService.generateBatchId();

        List<UserIngredientCreateResponseDto> results = requests.stream()
                .map(req -> createOne(user, req, batchId))
                .toList();

        // 이번 등록된 ingredientId 목록 수집
        List<Long> newIngredientIds = results.stream()
                .map(UserIngredientCreateResponseDto::getIngredientId)
                .toList();

        // 변경된 saveBatch 호출
        recentIngredientService.saveBatch(userId, newIngredientIds);

        // 최초 재료 등록 온보딩 쿠키 지급 (일회성)
        boolean rewardGranted = grantFirstIngredientRewardIfEligible(user);

        return UserIngredientListCreateResponseDto.of(results, rewardGranted);
    }

    private UserIngredientCreateResponseDto createOne(
            User user,
            UserIngredientCreateRequestDto req,
            String batchId) {

        if (req.getType() == Type.DEFAULT) {
            return createFromDefault(user, req, batchId);
        } else {
            return createFromCustom(user, req, batchId);
        }

    }

    private UserIngredientCreateResponseDto createFromDefault(
            User user,
            UserIngredientCreateRequestDto req,
            String batchId) {

        DefaultIngredient ref = defaultIngredientRepository.findById(req.getReferenceId())
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

        int quantity = req.getQuantity()!= null ? req.getQuantity():DEFAULT_QUANTITY;
        Unit unit = req.getUnit()!= null ? req.getUnit():ref.getUnit();
        Storage storage = req.getStorage()!= null ? req.getStorage():ref.getDefaultStorage();
        LocalDate expiration = req.getExpirationDate()!= null ? req.getExpirationDate():calcExpiration(ref.getDefaultExpirationDays());
        String memo = req.getMemo();

        UserIngredient entity = UserIngredient.builder()
                .user(user)
                .type(Type.DEFAULT)
                .referenceId(ref.getId())
                .quantity(quantity)
                .unit(unit)
                .storage(storage)
                .expirationDate(expiration)
                .memo(memo)
                .batchId(batchId)
                .build();

        UserIngredient saved = userIngredientRepository.save(entity);

        // 주간 소비 리포트용 로그 생성
        consumptionReportService.createLogForNewIngredient(user, saved);

        return UserIngredientCreateResponseDto.of(entity, ref.getIngredient(), ref.getImageUrl());
    }

    private UserIngredientCreateResponseDto createFromCustom(
            User user,
            UserIngredientCreateRequestDto req,
            String batchId
            ) {

        CustomIngredient ref = customIngredientRepository
                .findByIdAndUserId(req.getReferenceId(), user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_REFERENCE_NOT_FOUND));

        int quantity = req.getQuantity()!= null ? req.getQuantity():DEFAULT_QUANTITY;
        Unit unit = req.getUnit()!= null ? req.getUnit():DEFAULT_CUSTOM_UNIT;
        Storage storage = req.getStorage()!= null ? req.getStorage():ref.getStorage();
        LocalDate expiration = req.getExpirationDate() != null ? req.getExpirationDate():calcExpiration(ref.getExpirationDays());
        String memo = req.getMemo();

        UserIngredient entity = UserIngredient.builder()
                .user(user)
                .type(Type.CUSTOM)
                .referenceId(ref.getId())
                .quantity(quantity)
                .unit(unit)
                .storage(storage)
                .expirationDate(expiration)
                .memo(memo)
                .batchId(batchId)
                .build();

        UserIngredient saved = userIngredientRepository.save(entity);

        // 주간 소비 리포트용 로그 생성
        consumptionReportService.createLogForNewIngredient(user, saved);

        return UserIngredientCreateResponseDto.of(entity, ref.getName(), ref.getImageUrl());
    }

    private LocalDate calcExpiration(Integer expirationDays) {
        if (expirationDays == null) return null;
        return LocalDate.now().plusDays(expirationDays);
    }

    // 온보딩 재료 추가 여부 확인
    private boolean grantFirstIngredientRewardIfEligible(User user) {
        if (!user.isFirstIngredientReward()) {
            cookieService.updateCookie(user.getUserId(), CookieLog.CookieLogType.ONBOARDING_INGREDIENT);
            user.markFirstIngredientRewarded();
            return true;
        }
        return false;
    }

}
