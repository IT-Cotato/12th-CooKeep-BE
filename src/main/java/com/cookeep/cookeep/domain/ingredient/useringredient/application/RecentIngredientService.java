package com.cookeep.cookeep.domain.ingredient.useringredient.application;

import com.cookeep.cookeep.api.dto.response.RecentIngredientsResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.common.domain.Type;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.RecentIngredientBatchRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.RecentIngredientBatch;
import com.cookeep.cookeep.domain.ingredient.useringredient.entity.UserIngredient;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentIngredientService {

    private static final int MAX_RECENT_COUNT = 6;

    private final RecentIngredientBatchRepository batchRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 최근 추가한 순으로 최대 6개 재료 목록 반환 (첫 등록은 빈 리스트)
    public RecentIngredientsResponseDto getRecentIngredients(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Optional<RecentIngredientBatch> batchOpt = batchRepository.findByUser_UserId(userId);

        if (batchOpt.isEmpty() || batchOpt.get().getIngredientIdsJson() == null) {
            return RecentIngredientsResponseDto.builder()
                    .ingredients(List.of())
                    .build();
        }

        List<Long> ingredientIds = parseIngredientIds(batchOpt.get().getIngredientIdsJson());
        if (ingredientIds.isEmpty()) {
            return RecentIngredientsResponseDto.builder()
                    .ingredients(List.of())
                    .build();
        }

        // ingredientId로 UserIngredient 조회
        List<UserIngredient> ingredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(ingredientIds, userId);

        // batch에 저장된 순서(ingredientId 내림차순) 그대로 정렬
        Map<Long, UserIngredient> ingredientMap = ingredients.stream()
                .collect(Collectors.toMap(UserIngredient::getIngredientId, ui -> ui));

        List<UserIngredient> ordered = ingredientIds.stream()
                .map(ingredientMap::get)
                .filter(Objects::nonNull) // 삭제된 재료는 제외
                .toList();

        return RecentIngredientsResponseDto.builder()
                .ingredients(toItems(ordered))
                .build();
    }

    // 배치 저장: 재료 등록 완료 후 호출하여 최신 batchId를 업데이트
    @Transactional
    public void saveBatch(Long userId, List<Long> newIngredientIds) {
        if (newIngredientIds == null || newIngredientIds.isEmpty()) return;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Optional<RecentIngredientBatch> existingBatch = batchRepository.findByUser_UserId(userId);

        // 이번에 등록된 UserIngredient 조회
        List<UserIngredient> newIngredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(newIngredientIds, userId);

        // 기존 배치의 ingredientId 목록 로드
        List<Long> existingIds = existingBatch
                .map(b -> parseIngredientIds(b.getIngredientIdsJson()))
                .orElse(List.of());

        List<UserIngredient> existingIngredients =
                userIngredientRepository.findAllByIngredientIdInAndUser_UserId(existingIds, userId);

        // 전체 후보 목록: 새것 + 기존것 (새것이 앞에 와야 중복 제거 시 최신이 살아남음)
        // ingredientId 내림차순 정렬 후 referenceId 중복 제거
        Map<String, UserIngredient> deduped = new LinkedHashMap<>();

        // ingredientId DESC 정렬 후 LinkedHashMap에 삽입 → 삽입 순서가 곧 최종 순서
        Stream.concat(newIngredients.stream(), existingIngredients.stream())
                .sorted(Comparator.comparingLong(UserIngredient::getIngredientId).reversed())
                .forEach(ui -> {
                    String key = ui.getType().name() + "_" + ui.getReferenceId();
                    deduped.putIfAbsent(key, ui);
                });

        // ingredientId 내림차순으로 최대 6개
        List<Long> finalIds = deduped.values().stream()
                .limit(MAX_RECENT_COUNT)
                .map(UserIngredient::getIngredientId)
                .toList();

        String idsJson = toJson(finalIds);
        String batchId = generateBatchId();

        if (existingBatch.isEmpty()) {
            batchRepository.save(
                    RecentIngredientBatch.builder()
                            .user(user)
                            .batchId(batchId)
                            .ingredientIdsJson(idsJson)
                            .build()
            );
        } else {
            existingBatch.get().update(batchId, idsJson);
        }
    }

    // UUID 생성하여 반환. createAll() 에서 배치 시작 시 호출
    public static String generateBatchId() {
        return UUID.randomUUID().toString();
    }

    private List<Long> parseIngredientIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Long.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<Long> ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<RecentIngredientsResponseDto.RecentIngredientItem> toItems(List<UserIngredient> ingredients) {

        // referenceId를 타입별로 분리
        List<Long> defaultIds = ingredients.stream()
                .filter(ui -> ui.getType() == Type.DEFAULT)
                .map(UserIngredient::getReferenceId)
                .toList();

        List<Long> customIds = ingredients.stream()
                .filter(ui -> ui.getType() == Type.CUSTOM)
                .map(UserIngredient::getReferenceId)
                .toList();

        // 타입별 1번씩만 조회 후 Map으로 변환
        Map<Long, DefaultIngredient> defaultMap = defaultIngredientRepository
                .findAllById(defaultIds).stream()
                .collect(Collectors.toMap(DefaultIngredient::getId, d -> d));
        Map<Long, CustomIngredient> customMap = customIngredientRepository
                .findAllById(customIds).stream()
                .collect(Collectors.toMap(CustomIngredient::getId, c -> c));

        // 매핑
        return ingredients.stream().map(ui -> {
            String name;
            String imageUrl;
            if (ui.getType() == Type.DEFAULT) {
                DefaultIngredient ref = defaultMap.get(ui.getReferenceId());
                name = ref != null ? ref.getIngredient() : "Unknown";
                imageUrl = ref != null ? ref.getImageUrl() : "";
            } else {
                CustomIngredient ref = customMap.get(ui.getReferenceId());
                name = ref != null ? ref.getName() : "Unknown";
                imageUrl = ref != null ? ref.getImageUrl() : "";
            }
            return RecentIngredientsResponseDto.RecentIngredientItem.builder()
                    .ingredientId(ui.getIngredientId())
                    .type(ui.getType().name())
                    .name(name)
                    .imageUrl(imageUrl)
                    .build();
        }).toList();
    }

}
