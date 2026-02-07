package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.user.dao.UserRepository;
import com.cookeep.cookeep.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecipeService {

    private final DailyRecipeRepository dailyRecipeRepository;
    private final AiRecipeRepository aiRecipeRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 채택된 AI 레시피 목록 조회 (레시피 선택 화면)
    @Transactional(readOnly = true)
    public List<AiRecipe> getAdoptedAiRecipes(Long userId) {
        return aiRecipeRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    // 데일리 레시피 등록
    public DailyRecipe createDailyRecipe(Long userId, Long aiRecipeId, String title,
                                         String description, String recipeImageUrl, Boolean isPublic) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        AiRecipe aiRecipe = aiRecipeRepository.findById(aiRecipeId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RECIPE_NOT_FOUND));

        // 본인의 AI 레시피인지 확인
        if (!aiRecipe.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.DAILY_RECIPE_FORBIDDEN);
        }

        // AI 레시피 내용 스냅샷 생성
        String content = buildContentSnapshot(aiRecipe);

        // title: 사용자 지정값이 있으면 사용, 없으면 AI 레시피 제목
        String resolvedTitle = (title != null && !title.isBlank()) ? title : aiRecipe.getTitle();

        DailyRecipe dailyRecipe = DailyRecipe.builder()
                .title(resolvedTitle)
                .description(description)
                .content(content)
                .recipeImageUrl(recipeImageUrl)
                .isPublic(isPublic != null ? isPublic : false)
                .user(user)
                .aiRecipe(aiRecipe)
                .build();

        return dailyRecipeRepository.save(dailyRecipe);
    }

    // 내 데일리 레시피 목록 조회
    @Transactional(readOnly = true)
    public List<DailyRecipe> getMyDailyRecipes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return dailyRecipeRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    // 데일리 레시피 상세 조회
    @Transactional(readOnly = true)
    public DailyRecipe getDailyRecipeDetail(Long userId, Long dailyRecipeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return dailyRecipeRepository.findByIdAndUser(dailyRecipeId, user)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));
    }

    // AI 레시피 데이터를 하나의 JSON 문자열로 합쳐 스냅샷 생성
    private String buildContentSnapshot(AiRecipe aiRecipe) {
        try {
            var contentMap = new LinkedHashMap<String, Object>();
            contentMap.put("ingredients", objectMapper.readTree(aiRecipe.getIngredientsJson()));
            contentMap.put("steps", objectMapper.readTree(aiRecipe.getStepsJson()));
            if (aiRecipe.getYoutubeUrlJson() != null) {
                contentMap.put("youtubeReferences", objectMapper.readTree(aiRecipe.getYoutubeUrlJson()));
            }
            return objectMapper.writeValueAsString(contentMap);
        } catch (Exception e) {
            log.error("AI 레시피 내용 스냅샷 생성 실패", e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
