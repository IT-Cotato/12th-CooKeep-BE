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

import java.util.*;
import java.util.stream.Collectors;

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

    // 최근 추가한 순으로 최대 6개 재료 목록 반환 (첫 등록은 빈 리스트)
    public RecentIngredientsResponseDto getRecentIngredients(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        List<RecentIngredientBatch> batches =
                batchRepository.findByUser_UserIdOrderByCreatedAtDesc(userId);

        if (batches.isEmpty()) {
            return RecentIngredientsResponseDto.builder()
                    .ingredients(List.of())
                    .build();
        }

        // 1단계: 최신 배치부터 역순으로 총 6개 행 수집 (중복 포함)
        List<UserIngredient> collectedRaw = new ArrayList<>();

        for (RecentIngredientBatch batch : batches) {
            if (collectedRaw.size() >= MAX_RECENT_COUNT) break;

            List<UserIngredient> batchIngredients =
                    userIngredientRepository.findByUserIdAndBatchId(userId, batch.getBatchId());

            for (UserIngredient ui : batchIngredients) {
                collectedRaw.add(ui);
                if (collectedRaw.size() >= MAX_RECENT_COUNT) break;
            }
        }

        // 2단계: type + referenceId 기준 중복 제거 (순서 유지)
        Set<String> seenKeys = new LinkedHashSet<>();
        List<UserIngredient> deduplicated = new ArrayList<>();

        for (UserIngredient ui : collectedRaw) {
            String key = ui.getType().name() + "_" + ui.getReferenceId();
            if (seenKeys.add(key)) { // add()는 중복이면 false 반환
                deduplicated.add(ui);
            }
        }

        // 3단계: DTO 변환
        List<RecentIngredientsResponseDto.RecentIngredientItem> items = toItems(deduplicated);

        return RecentIngredientsResponseDto.builder()
                .ingredients(items)
                .build();
    }

    // 배치 저장: 재료 등록 완료 후 호출하여 최신 batchId를 업데이트
    @Transactional
    public void saveBatch(Long userId, String batchId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 기존 upsert 방식에서 항상 새 행 INSERT로 변경
        batchRepository.save(
                RecentIngredientBatch.builder()
                        .user(user)
                        .batchId(batchId)
                        .build()
        );
    }

    // UUID 생성하여 반환. createAll() 에서 배치 시작 시 호출
    public static String generateBatchId() {
        return UUID.randomUUID().toString();
    }

    private List<RecentIngredientsResponseDto.RecentIngredientItem> toItems(List<UserIngredient> ingredients) {

        // referenceId를 타입별로 분리
        List<Long> defaultIds = ingredients.stream()
                .filter(ui -> ui.getType() == Type.DEFAULT)
                .map(UserIngredient::getReferenceId)
                .distinct()
                .toList();

        List<Long> customIds = ingredients.stream()
                .filter(ui -> ui.getType() == Type.CUSTOM)
                .map(UserIngredient::getReferenceId)
                .distinct()
                .toList();

        // 타입별 1번씩만 조회 후 Map으로 변환
        Map<Long, DefaultIngredient> defaultMap = defaultIngredientRepository
                .findAllById(defaultIds).stream()
                .collect(Collectors.toMap(DefaultIngredient::getId, d -> d));

        Map<Long, CustomIngredient> customMap = customIngredientRepository
                .findAllById(customIds).stream()
                .collect(Collectors.toMap(CustomIngredient::getId, c -> c));

        // 매핑
        return ingredients.stream()
                .map(ui -> {
                    String name;
                    String imageUrl;

                    if (ui.getType() == Type.DEFAULT) {
                        DefaultIngredient ref = defaultMap.get(ui.getReferenceId());
                        imageUrl = ref.getImageUrl();
                        name = ref.getIngredient();
                    } else {
                        CustomIngredient ref = customMap.get(ui.getReferenceId());
                        name = ref.getName();
                        imageUrl = ref.getImageUrl();
                    }

                    return RecentIngredientsResponseDto.RecentIngredientItem.builder()
                            .ingredientId(ui.getIngredientId())
                            .type(ui.getType().name())
                            .name(name)
                            .imageUrl(imageUrl)
                            .build();
                })
                .toList();
    }

}
