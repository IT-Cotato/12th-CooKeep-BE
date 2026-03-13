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

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentIngredientService {

    private final RecentIngredientBatchRepository batchRepository;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;
    private final UserRepository userRepository;

    // 유저의 직전 배치에 해당하는 재료 목록 반환 (첫 등록은 빈 리스트)
    public RecentIngredientsResponseDto getRecentIngredients(Long userId) {
        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // 최신 batchId 조회; 없으면 빈 결과 반환
        return batchRepository.findByUser_UserId(userId)
                .map(batch -> buildResponse(userId, batch.getBatchId()))
                .orElseGet(() -> RecentIngredientsResponseDto.builder()
                        .ingredients(List.of())
                        .build());
    }

    // 배치 저장: 재료 등록 완료 후 호출하여 최신 batchId를 업데이트
    @Transactional
    public void saveBatch(Long userId, String batchId) {
        batchRepository.findByUser_UserId(userId)
                .ifPresentOrElse(
                        existing -> existing.updateBatchId(batchId),
                        () -> {
                            User user = userRepository.findById(userId)
                                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
                            batchRepository.save(
                                    RecentIngredientBatch.builder()
                                            .user(user)
                                            .batchId(batchId)
                                            .build()
                            );
                        }
                );
    }

    // UUID 생성하여 반환. createAll() 에서 배치 시작 시 호출
    public static String generateBatchId() {
        return UUID.randomUUID().toString();
    }

    // 내부 메서드
    private RecentIngredientsResponseDto buildResponse(Long userId, String batchId) {
        List<UserIngredient> ingredients =
                userIngredientRepository.findByUserIdAndBatchId(userId, batchId);

        List<RecentIngredientsResponseDto.RecentIngredientItem> items = ingredients.stream()
                .map(this::toItem)
                .toList();

        return RecentIngredientsResponseDto.builder()
                .ingredients(items)
                .build();
    }

    private RecentIngredientsResponseDto.RecentIngredientItem toItem(UserIngredient ui) {
        String name;
        String imageUrl;

        if (ui.getType() == Type.DEFAULT) {
            DefaultIngredient ref = defaultIngredientRepository
                    .findById(ui.getReferenceId())
                    .orElse(null);
            name = ref != null ? ref.getIngredient() : "Unknown";
            imageUrl = ref != null ? ref.getImageUrl() : "";
        } else {
            CustomIngredient ref = customIngredientRepository
                    .findById(ui.getReferenceId())
                    .orElse(null);
            name = ref != null ? ref.getName() : "Unknown";
            imageUrl = ref != null ? ref.getImageUrl() : "";
        }

        return RecentIngredientsResponseDto.RecentIngredientItem.builder()
                .ingredientId(ui.getIngredientId())
                .type(ui.getType().name())
                .name(name)
                .imageUrl(imageUrl)
                .build();
    }

}
