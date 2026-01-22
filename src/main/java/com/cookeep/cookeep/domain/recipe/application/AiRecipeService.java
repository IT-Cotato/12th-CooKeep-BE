package com.cookeep.cookeep.domain.recipe.application;

import com.cookeep.cookeep.common.exception.AppException;
import com.cookeep.cookeep.common.exception.ErrorCode;
import com.cookeep.cookeep.domain.ingredient.customingredient.dao.CustomIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.customingredient.entity.CustomIngredient;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.dao.DefaultIngredientRepository;
import com.cookeep.cookeep.domain.ingredient.defaultingredient.entity.DefaultIngredient;
import com.cookeep.cookeep.domain.ingredient.useringredient.dao.UserIngredientRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiMessageRepository;
import com.cookeep.cookeep.domain.recipe.dao.AiSessionRepository;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeRequestDto;
import com.cookeep.cookeep.domain.recipe.dto.AiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.GeminiRecipeResponseDto;
import com.cookeep.cookeep.domain.recipe.dto.IngredientDetailDto;
import com.cookeep.cookeep.domain.recipe.entity.AiMessage;
import com.cookeep.cookeep.domain.recipe.entity.AiSession;
import com.cookeep.cookeep.domain.recipe.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AiRecipeService {

    private static final int MAX_CHANGE_COUNT = 5;

    private final GeminiService geminiService;
    private final AiSessionRepository aiSessionRepository;
    private final AiMessageRepository aiMessageRepository;
    private final ObjectMapper objectMapper;
    private final UserIngredientRepository userIngredientRepository;
    private final DefaultIngredientRepository defaultIngredientRepository;
    private final CustomIngredientRepository customIngredientRepository;

    public AiRecipeResponseDto generateRecipe(
            Long userId,
            AiRecipeRequestDto request
    ) {

        // 1. 요청 검증
        validateRequest(request);

        // 2. 세션 조회 or 생성
        AiSession session = getOrCreateSession(userId, request);

        // 3. 세션 완료 여부 확인
        if (session.getIsCompleted()) {
            throw new AppException(ErrorCode.SESSION_ALREADY_COMPLETED);
        }

        // 4. 변경 횟수 제한 체크
        if (session.isChangeLimitExceeded(MAX_CHANGE_COUNT)) {
            throw new AppException(ErrorCode.AI_RECIPE_CHANGE_LIMIT_EXCEEDED);
        }

        // 5. 시도 횟수 증가
        session.incrementAttemptNumber();

        // 6. USER 프리셋 메시지 저장
        saveUserMessage(session);

        // 7. 재료 이름 enrichment
        List<IngredientDetailDto> enrichedIngredients =
                enrichIngredientsWithNames(request.getIngredients());

        // 8. Gemini 호출
        GeminiRecipeResponseDto geminiResponse =
                geminiService.generateRecipe(
                        enrichedIngredients,
                        request.getDifficulty()
                );

        // 9. AI 메시지 저장
        saveAiMessage(session, geminiResponse);

        // 10. 세션 제목 갱신
        session.updateTitle(geminiResponse.getTitle());

        return AiRecipeResponseDto.builder()
                .sessionId(session.getId())
                .changeCount(session.getAttemptNumber())
                .recipe(geminiResponse)
                .build();
    }

    private void validateRequest(AiRecipeRequestDto request) {
        // 재료 목록 검증
        if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
            throw new AppException(ErrorCode.INGREDIENTS_REQUIRED);
        }

        // 난이도 검증
        if (request.getDifficulty() == null) {
            throw new AppException(ErrorCode.INVALID_DIFFICULTY);
        }
    }

    // 내부 메서드
    private AiSession getOrCreateSession(Long userId, AiRecipeRequestDto request) {

        if (request.getSessionId() == null) {
            return aiSessionRepository.save(
                    AiSession.builder()
                            .userId(userId)
                            .difficulty(request.getDifficulty())
                            .attemptNumber(0)
                            .isPinned(false)
                            .isCompleted(false)
                            .build()
            );
        }

        return aiSessionRepository.findByIdAndUserId(
                        request.getSessionId(), userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.AI_SESSION_NOT_FOUND));
    }

    private void saveUserMessage(AiSession session) {
        aiMessageRepository.save(
                AiMessage.builder()
                        .session(session)
                        .role(Role.USER)
                        .content("다른 레시피를 받을래요")
                        .build()
        );
    }

    private void saveAiMessage(
            AiSession session,
            GeminiRecipeResponseDto response
    ) {
        try {
            String json = objectMapper.writeValueAsString(response);

            aiMessageRepository.save(
                    AiMessage.builder()
                            .session(session)
                            .role(Role.AI)
                            .content(json)
                            .build()
            );
        } catch (Exception e) {
            log.error("AI 메시지 저장 실패", e);
            throw new AppException(ErrorCode.AI_SEARCH_FAILED);
        }
    }

    // 재료 이름 채우기
    private List<IngredientDetailDto> enrichIngredientsWithNames(
            List<IngredientDetailDto> ingredients
    ) {
        return ingredients.stream()
                .map(this::enrichIngredientWithName)
                .collect(Collectors.toList());
    }

    private IngredientDetailDto enrichIngredientWithName(IngredientDetailDto dto) {
        String name = getIngredientName(dto.getType(), dto.getReferenceId());

        // DTO를 불변으로 만들거나, 빌더 패턴 사용
        return IngredientDetailDto.builder()
                .type(dto.getType())
                .referenceId(dto.getReferenceId())
                .name(name)
                .quantity(dto.getQuantity())
                .unit(dto.getUnit())
                .build();
    }

    private String getIngredientName(String type, Long referenceId) {
        if ("DEFAULT".equals(type)) {
            return defaultIngredientRepository.findById(referenceId)
                    .map(DefaultIngredient::getIngredient)
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
        } else if ("CUSTOM".equals(type)) {
            return customIngredientRepository.findById(referenceId)
                    .map(CustomIngredient::getName)
                    .orElseThrow(() -> new AppException(ErrorCode.INGREDIENT_NOT_FOUND));
        }
        throw new AppException(ErrorCode.INVALID_INGREDIENT_TYPE);
    }
}
