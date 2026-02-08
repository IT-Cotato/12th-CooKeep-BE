package com.cookeep.cookeep.domain.dailyrecipe.application;

import com.cookeep.cookeep.api.dto.response.DailyRecipeCalendarResponseDto;
import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.dailyrecipe.dao.DailyRecipeRepository;
import com.cookeep.cookeep.domain.dailyrecipe.entity.DailyRecipe;
import com.cookeep.cookeep.domain.recipe.dao.AiRecipeRepository;
import com.cookeep.cookeep.domain.recipe.entity.AiRecipe;
import com.cookeep.cookeep.domain.user.application.UserReader;
import com.cookeep.cookeep.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DailyRecipeService {

    private final DailyRecipeRepository dailyRecipeRepository;
    private final AiRecipeRepository aiRecipeRepository;
    private final UserReader userReader;
    private final ObjectMapper objectMapper;

    // 채택된 AI 레시피 목록 조회 (레시피 선택 화면)
    @Transactional(readOnly = true)
    public List<AiRecipe> getAdoptedAiRecipes(Long userId) {
        return aiRecipeRepository.findAllByUserIdOrderByCreatedAtDesc(userId); // 최신순 조회
    }

    // 채택된 AI 레시피 상세 조회 (레시피 선택 후 내용 확인)
    @Transactional(readOnly = true)
    public AiRecipe getAdoptedAiRecipeDetail(Long userId, Long aiRecipeId) {
        return aiRecipeRepository.findByIdAndUserId(aiRecipeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.AI_RECIPE_NOT_FOUND));
    }

    // 데일리 레시피 등록
    public DailyRecipe createDailyRecipe(Long userId, Long aiRecipeId, String title,
                                         String description, String recipeImageUrl, Boolean isPublic) {
        User user = userReader.readById(userId);

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

    // 데일리 레시피 상세 조회
    @Transactional(readOnly = true)
    public DailyRecipe getDailyRecipeDetail(Long userId, Long dailyRecipeId) {
        User user = userReader.readById(userId);

        return dailyRecipeRepository.findByIdAndUser(dailyRecipeId, user)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));
    }

    // 데일리 레시피 수정 (제목, 한줄평)
    public DailyRecipe updateDailyRecipe(Long userId, Long dailyRecipeId, String title, String description) {
        if ((title == null || title.isBlank()) && description == null) {
            throw new AppException(ErrorCode.DAILY_RECIPE_UPDATE_FIELDS_REQUIRED);
        }

        User user = userReader.readById(userId);

        DailyRecipe dailyRecipe = dailyRecipeRepository.findByIdAndUser(dailyRecipeId, user)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

        dailyRecipe.updateTitleAndDescription(title, description);
        return dailyRecipe;
    }

    // 데일리 레시피 공개 범위 수정
    public DailyRecipe updateDailyRecipeVisibility(Long userId, Long dailyRecipeId, Boolean isPublic) {
        User user = userReader.readById(userId);

        DailyRecipe dailyRecipe = dailyRecipeRepository.findByIdAndUser(dailyRecipeId, user)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

        dailyRecipe.updateVisibility(isPublic);
        return dailyRecipe;
    }

    // 날짜 기반 데일리 레시피 목록 조회
    @Transactional(readOnly = true)
    public List<DailyRecipe> getDailyRecipesByDate(Long userId, LocalDate date) {
        User user = userReader.readById(userId);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return dailyRecipeRepository.findByUserAndDateRange(user, startOfDay, endOfDay);
    }

    // 캘린더 마킹용 월별 데일리 레시피 날짜 목록 조회
    @Transactional(readOnly = true)
    public List<DailyRecipeCalendarResponseDto> getCalendarMarking(Long userId, int year, int month) {
        User user = userReader.readById(userId);

        LocalDateTime start = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime end = start.plusMonths(1);

        List<DailyRecipe> recipes = dailyRecipeRepository.findByUserAndDateRangeAsc(user, start, end);

        // 날짜별 첫 번째 레시피 추출 (ASC 정렬이므로 putIfAbsent로 가장 먼저 등록된 레시피 선택)
        Map<LocalDate, DailyRecipe> firstPerDate = new LinkedHashMap<>();
        for (DailyRecipe recipe : recipes) {
            LocalDate date = recipe.getCreatedAt().toLocalDate();
            firstPerDate.putIfAbsent(date, recipe);
        }

        return firstPerDate.entrySet().stream()
                .map(entry -> DailyRecipeCalendarResponseDto.of(
                        entry.getKey(),
                        entry.getValue().getRecipeImageUrl()))
                .toList();
    }

    // 데일리 레시피 삭제
    public void deleteDailyRecipe(Long userId, Long dailyRecipeId) {
        User user = userReader.readById(userId);

        DailyRecipe dailyRecipe = dailyRecipeRepository.findByIdAndUser(dailyRecipeId, user)
                .orElseThrow(() -> new AppException(ErrorCode.DAILY_RECIPE_NOT_FOUND));

        dailyRecipeRepository.delete(dailyRecipe);
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
