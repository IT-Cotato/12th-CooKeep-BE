package com.cookeep.cookeep.domain.refrigerator.application;

import com.cookeep.cookeep.api.dto.response.PaginatedIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.RefrigeratorIngredientsResponseDto;
import com.cookeep.cookeep.api.dto.response.RefrigeratorSearchResponseDto;
import com.cookeep.cookeep.api.dto.response.UserIngredientDetailResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
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
import com.cookeep.cookeep.domain.refrigerator.entity.IngredientSort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // 1. 냉장고 전체 식재료 조회 (보관 장소별 그룹화)
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

    // 2. 전체보기 페이지 내 정렬
    public PaginatedIngredientsResponseDto getIngredientsByStorage(
            Long userId,
            Storage storage,
            IngredientSort sort,
            int page,
            int size) {

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (storage == null) {
            throw new AppException(ErrorCode.REFRIGERATOR_INVALID_QUERY);
        }

        // 정렬 기준 설정
        Sort sortCriteria = getSortCriteria(sort);
        Pageable pageable = PageRequest.of(page, size, sortCriteria);

        // 페이지네이션 조회
        Page<UserIngredient> ingredientPage = userIngredientRepository
                .findByUser_UserIdAndStorage(userId, storage, pageable);

        List<RefrigeratorIngredientsResponseDto.IngredientItem> items =
                convertToIngredientItems(ingredientPage.getContent());

        return PaginatedIngredientsResponseDto.builder()
                .content(items)
                .page(ingredientPage.getNumber())
                .size(ingredientPage.getSize())
                .hasNext(ingredientPage.hasNext())
                .build();
    }

    // 3. 식재료 상세 조회
    public UserIngredientDetailResponseDto getIngredientDetail(Long userId, Long ingredientId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        UserIngredient userIngredient = userIngredientRepository
                .findByIngredientIdAndUserId(ingredientId, userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.INGREDIENT_NOT_FOUND));

        // 식재료 이름/팁/이미지 조회
        String ingredientName;
        String aiTip = null;
        String imageUrl;

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
            // 커스텀 식재료는 aiTip 없음
        }

        return UserIngredientDetailResponseDto.builder()
                .ingredientId(userIngredient.getIngredientId())
                .name(ingredientName)
                .storage(userIngredient.getStorage())
                .expirationDate(userIngredient.getExpirationDate())
                .quantity(userIngredient.getQuantity())
                .unit(userIngredient.getUnit())
                .leftDays(userIngredient.getLeftDays())
                .memo(userIngredient.getMemo())
                .aiTip(aiTip)
                .imageUrl(imageUrl)
                .createdAt(userIngredient.getCreatedAt().toLocalDate())
                .build();
    }

    // 4. 식재료 검색
    public RefrigeratorSearchResponseDto searchIngredients(
            Long userId,
            String searchQuery,
            Storage storage,
            IngredientSort sort,
            int page,
            int size) {

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 검색어 확인
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            throw new AppException(ErrorCode.REFRIGERATOR_SEARCH_QUERY_REQUIRED);
        }

        String processedQuery = searchQuery.trim();

        // 정렬 기준 설정
        Sort sortCriteria = getSortCriteria(sort);
        Pageable pageable = PageRequest.of(page, size, sortCriteria);

        // 검색 실행
        Page<UserIngredient> searchResults = (storage != null)
                ? userIngredientRepository.searchIngredientsWithStorage(userId, processedQuery, storage, pageable)
                : userIngredientRepository.searchIngredientsWithoutStorage(userId, processedQuery, pageable);

        // 결과 변환
        List<RefrigeratorSearchResponseDto.SearchResultItem> items = searchResults.getContent()
                .stream()
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

                    return RefrigeratorSearchResponseDto.SearchResultItem.builder()
                            .ingredientId(ui.getIngredientId())
                            .name(name)
                            .imageUrl(imageUrl)
                            .storage(ui.getStorage())
                            .expirationDate(ui.getExpirationDate())
                            .quantity(ui.getQuantity())
                            .unit(ui.getUnit())
                            .build();
                })
                .collect(Collectors.toList());

        return RefrigeratorSearchResponseDto.builder()
                .content(items)
                .page(searchResults.getNumber())
                .size(searchResults.getSize())
                .hasNext(searchResults.hasNext())
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
                            .ingredientId(ui.getIngredientId())
                            .name(name)
                            .leftDays(ui.getLeftDays())
                            .imageUrl(imageUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 정렬
    private Sort getSortCriteria(IngredientSort sort) {
        if (sort == null) {
            sort = IngredientSort.EXPIRATION_ASC; // 기본값: 유통기한 임박순
        }

        return switch (sort) {
            case EXPIRATION_ASC -> Sort.by(Sort.Direction.ASC, "leftDays");
            case CREATED_DESC -> Sort.by(Sort.Direction.DESC, "createdAt");
            case CREATED_ASC -> Sort.by(Sort.Direction.ASC, "createdAt");
        };
    }

    // 식재료 이름 조회
    private String getIngredientName(UserIngredient userIngredient) {
        if (userIngredient.getType() == Type.DEFAULT) {
            return defaultIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .map(DefaultIngredient::getIngredient)
                    .orElse("Unknown");
        } else {
            return customIngredientRepository
                    .findById(userIngredient.getReferenceId())
                    .map(CustomIngredient::getName)
                    .orElse("Unknown");
        }
    }
}
